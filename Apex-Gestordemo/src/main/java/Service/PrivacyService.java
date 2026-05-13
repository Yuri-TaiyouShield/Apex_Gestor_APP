package Service;

import DTO.ClienteExportDTO;
import DTO.ConsentRequestDTO;
import DTO.PrivacyRequestDTO;
import Model.Cliente;
import Model.ConsentAudit;
import Model.DataErasureLog;
import Model.PrivacyRequest;
import Repository.ClienteRepository;
import Repository.ConsentAuditRepository;
import Repository.DataErasureLogRepository;
import Repository.PrivacyRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PrivacyService {

    private final ConsentAuditRepository consentAuditRepository;
    private final PrivacyRequestRepository privacyRequestRepository;
    private final DataErasureLogRepository dataErasureLogRepository;
    private final ClienteRepository clienteRepository;
    private final DataProtectionService dataProtectionService;

    public PrivacyService(
            ConsentAuditRepository consentAuditRepository,
            PrivacyRequestRepository privacyRequestRepository,
            DataErasureLogRepository dataErasureLogRepository,
            ClienteRepository clienteRepository,
            DataProtectionService dataProtectionService
    ) {
        this.consentAuditRepository = consentAuditRepository;
        this.privacyRequestRepository = privacyRequestRepository;
        this.dataErasureLogRepository = dataErasureLogRepository;
        this.clienteRepository = clienteRepository;
        this.dataProtectionService = dataProtectionService;
    }

    public ConsentAudit registerConsent(ConsentRequestDTO request, HttpServletRequest servletRequest) {
        ConsentAudit audit = new ConsentAudit();
        audit.setTitularId(request.titularId());
        audit.setTipoTitular(request.tipoTitular());
        audit.setDocumentoHash(dataProtectionService.hash(request.documento()));
        audit.setVersao(request.versao());
        audit.setCanal(request.canal());
        audit.setAceitoEm(LocalDateTime.now());
        audit.setIpHash(dataProtectionService.hash(clientIp(servletRequest)));
        audit.setUserAgentHash(dataProtectionService.hash(servletRequest.getHeader("User-Agent")));
        return consentAuditRepository.save(audit);
    }

    public PrivacyRequest createPrivacyRequest(PrivacyRequestDTO request, Authentication authentication) {
        PrivacyRequest privacyRequest = new PrivacyRequest();
        privacyRequest.setProtocolo("LGPD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        privacyRequest.setTitularId(request.titularId());
        privacyRequest.setTipo(request.tipo().toUpperCase());
        privacyRequest.setStatus("PENDENTE");
        privacyRequest.setSolicitadoEm(LocalDateTime.now());
        privacyRequest.setSolicitanteLogin(authentication != null ? authentication.getName() : "anonymous");
        return privacyRequestRepository.save(privacyRequest);
    }

    public ClienteExportDTO exportCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente nao encontrado"));
        List<ClienteExportDTO.Consentimento> consentimentos = consentAuditRepository.findByTitularId(String.valueOf(id)).stream()
                .map(ClienteExportDTO.Consentimento::from)
                .toList();
        return new ClienteExportDTO(cliente, consentimentos, LocalDateTime.now());
    }

    @Transactional
    public Cliente anonymizeCliente(Long id, Authentication authentication) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente nao encontrado"));
        cliente.setNomeRazao("Titular removido " + id);
        cliente.setTelefone("00000000000");
        cliente.setCpfCnpj(String.format("ANON%010d", id).substring(0, 14));
        cliente.setTipoDocumento(0);
        cliente.setStatus(0);
        Cliente saved = clienteRepository.save(cliente);

        DataErasureLog log = new DataErasureLog();
        log.setTitularHash(dataProtectionService.hash(String.valueOf(id)));
        log.setMotivo("Direito ao esquecimento LGPD");
        log.setOperadorLogin(authentication != null ? authentication.getName() : "system");
        log.setExecutadoEm(LocalDateTime.now());
        dataErasureLogRepository.save(log);

        return saved;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
