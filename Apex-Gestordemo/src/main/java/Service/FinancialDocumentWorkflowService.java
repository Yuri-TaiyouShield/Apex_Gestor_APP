package Service;

import DTO.EmailDraftRequestDTO;
import DTO.FinancialDocumentDTO;
import DTO.FinancialDocumentRequestDTO;
import DTO.SignFinancialDocumentRequestDTO;
import Model.FinancialDigitalDocument;
import Repository.FinancialDigitalDocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FinancialDocumentWorkflowService {

    public static final String STATUS_PENDING_SIGNATURE = "PENDENTE_ASSINATURA";
    public static final String STATUS_SIGNED = "ASSINADO";
    public static final String STATUS_SENT = "ENVIADO";

    private static final Set<String> PRIVILEGED_SIGNER_ROLES = Set.of("ADMIN", "ADMINISTRACAO", "CONTADOR", "ADVOGADO");

    private final FinancialDigitalDocumentRepository repository;
    private final FinancialAuditTrailService auditTrailService;
    private final FinancialDocumentMessagingService messagingService;
    private final DataProtectionService dataProtectionService;

    public FinancialDocumentWorkflowService(
            FinancialDigitalDocumentRepository repository,
            FinancialAuditTrailService auditTrailService,
            FinancialDocumentMessagingService messagingService,
            DataProtectionService dataProtectionService
    ) {
        this.repository = repository;
        this.auditTrailService = auditTrailService;
        this.messagingService = messagingService;
        this.dataProtectionService = dataProtectionService;
    }

    @Transactional
    public FinancialDocumentDTO create(FinancialDocumentRequestDTO request, Authentication authentication) {
        String actor = actor(authentication);
        FinancialDigitalDocument document = new FinancialDigitalDocument();
        document.setTipoDocumento(request.tipoDocumento());
        document.setFuncionarioNome(request.funcionarioNome());
        document.setFuncionarioEmail(request.funcionarioEmail());
        document.setStatus(STATUS_PENDING_SIGNATURE);
        document.setCargoAssinanteObrigatorio(normalizeRole(defaultIfBlank(request.cargoAssinanteObrigatorio(), "CONTADOR")));
        document.setGeradoPor(actor);
        document.setGeradoEm(LocalDateTime.now());
        document.setConteudo(request.conteudo());
        document.setConteudoHash(dataProtectionService.hash(request.conteudo()));
        document.setReferencia(request.referencia());
        document.setAssuntoEmail("Documento " + request.tipoDocumento() + " aprovado - Apex Gestor");
        document.setMensagemEmail("Seu documento foi aprovado e assinado digitalmente no Apex Gestor.");
        FinancialDigitalDocument saved = repository.save(document);
        auditTrailService.record("DOCUMENTO_GERADO", "financial_digital_document", saved.getIdFinancialDigitalDocument(), actor, null, saved.getStatus(), saved);
        return toDto(saved);
    }

    @Transactional
    public FinancialDocumentDTO createDraft(Long id, EmailDraftRequestDTO request, Authentication authentication) {
        String actor = actor(authentication);
        FinancialDigitalDocument document = findDocument(id);
        String before = document.getAssuntoEmail();
        document.setAssuntoEmail(request.assunto());
        document.setMensagemEmail(request.mensagem());
        FinancialDigitalDocument saved = repository.save(document);
        auditTrailService.record("RASCUNHO_EMAIL_CRIADO", "financial_digital_document", saved.getIdFinancialDigitalDocument(), actor, before, saved.getAssuntoEmail(), saved);
        return toDto(saved);
    }

    @Transactional
    public FinancialDocumentDTO sign(Long id, SignFinancialDocumentRequestDTO request, Authentication authentication) {
        String actor = actor(authentication);
        FinancialDigitalDocument document = findDocument(id);
        if (!STATUS_PENDING_SIGNATURE.equals(document.getStatus())) {
            throw new IllegalStateException("Documento nao esta pendente de assinatura");
        }
        if (!request.aprovado()) {
            throw new IllegalStateException("Documento precisa ser aprovado antes da assinatura");
        }
        String signerRole = normalizeRole(request.cargoAssinante());
        ensureSignerAllowed(document.getCargoAssinanteObrigatorio(), signerRole, authentication);
        String before = document.getStatus();
        document.setAssinadoPor(request.nomeAssinante());
        document.setCargoAssinante(signerRole);
        document.setAssinadoEm(LocalDateTime.now());
        document.setAssinaturaDigitalHash(dataProtectionService.hash(document.getConteudoHash() + ":" + actor + ":" + signerRole + ":" + request.certificadoFingerprint()));
        document.setStatus(STATUS_SIGNED);
        try {
            if (messagingService.sendSignedDocument(document)) {
                document.setStatus(STATUS_SENT);
                document.setEnviadoEm(LocalDateTime.now());
            }
            document.setUltimoErro(null);
        } catch (RuntimeException ex) {
            document.setUltimoErro(dataProtectionService.maskSensitiveText(ex.getMessage()));
        }
        FinancialDigitalDocument saved = repository.save(document);
        auditTrailService.record("DOCUMENTO_ASSINADO", "financial_digital_document", saved.getIdFinancialDigitalDocument(), actor, before, saved.getStatus(), saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FinancialDocumentDTO> listRecent(int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        return repository.findAll(PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "geradoEm")))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private FinancialDigitalDocument findDocument(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Documento nao encontrado"));
    }

    private void ensureSignerAllowed(String requiredRole, String signerRole, Authentication authentication) {
        Set<String> authRoles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .map(this::normalizeRole)
                .collect(java.util.stream.Collectors.toSet());
        if (signerRole.equals(requiredRole) || PRIVILEGED_SIGNER_ROLES.contains(signerRole)) {
            if (authRoles.contains(signerRole) || authRoles.contains(requiredRole) || authRoles.stream().anyMatch(PRIVILEGED_SIGNER_ROLES::contains)) {
                return;
            }
        }
        throw new AccessDeniedException("Usuario autenticado nao possui cargo autorizado para assinar este documento");
    }

    private FinancialDocumentDTO toDto(FinancialDigitalDocument document) {
        return new FinancialDocumentDTO(
                document.getIdFinancialDigitalDocument(),
                document.getTipoDocumento(),
                document.getFuncionarioNome(),
                document.getFuncionarioEmail(),
                document.getStatus(),
                document.getCargoAssinanteObrigatorio(),
                document.getGeradoPor(),
                document.getGeradoEm(),
                document.getAssinadoPor(),
                document.getCargoAssinante(),
                document.getAssinadoEm(),
                document.getEnviadoEm(),
                document.getAssinaturaDigitalHash(),
                document.getAssuntoEmail(),
                document.getMensagemEmail(),
                document.getReferencia()
        );
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "anonymous" : authentication.getName();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeRole(String role) {
        String normalized = Normalizer.normalize(defaultIfBlank(role, "CONTADOR"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "_")
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "CONTADOR" : normalized;
    }
}
