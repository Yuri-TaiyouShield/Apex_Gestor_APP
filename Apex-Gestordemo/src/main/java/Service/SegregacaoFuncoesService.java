package Service;

import DTO.PoliticaSegregacaoRequestDTO;
import DTO.PoliticaSegregacaoResponseDTO;
import DTO.ValidacaoSegregacaoRequestDTO;
import DTO.ValidacaoSegregacaoResponseDTO;
import Model.PoliticaSegregacaoAprovacao;
import Model.Usuario;
import Repository.PoliticaSegregacaoAprovacaoRepository;
import Repository.UsuarioRepository;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import com.Apex.Apex_Gestordemo.security.SegregationOfDutiesViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SegregacaoFuncoesService {

    public static final String DOMINIO_ORCAMENTO = "ORCAMENTO_APROVACAO";
    public static final String DOMINIO_LGPD_ANONIMIZACAO = "LGPD_ANONIMIZACAO";
    public static final String DOMINIO_INVESTIMENTO = "INVESTIMENTO_APROVACAO";

    private static final int MIN_JUSTIFICATION_LENGTH = 10;

    private final PoliticaSegregacaoAprovacaoRepository politicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final PrivacyGuardService privacyGuardService;
    private final TenantScopeService tenantScopeService;
    private final TenantFeatureGuardService tenantFeatureGuardService;
    private final AuditTrailService auditTrailService;

    @Autowired
    public SegregacaoFuncoesService(
            PoliticaSegregacaoAprovacaoRepository politicaRepository,
            UsuarioRepository usuarioRepository,
            AuthorizationService authorizationService,
            PrivacyGuardService privacyGuardService,
            TenantScopeService tenantScopeService,
            TenantFeatureGuardService tenantFeatureGuardService,
            AuditTrailService auditTrailService) {
        this.politicaRepository = politicaRepository;
        this.usuarioRepository = usuarioRepository;
        this.authorizationService = authorizationService;
        this.privacyGuardService = privacyGuardService;
        this.tenantScopeService = tenantScopeService;
        this.tenantFeatureGuardService = tenantFeatureGuardService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<PoliticaSegregacaoResponseDTO> listarPoliticas(HttpServletRequest request) {
        tenantFeatureGuardService.requireFeature(request, ApexFeature.ADVANCED_FINANCE,
                "Governanca", "listar politicas de segregacao de funcoes");
        authorizationService.require(request, ApexPermission.BUDGET_VIEW,
                "Governanca", "listar politicas de segregacao de funcoes");
        Long empresaId = tenantScopeService.empresaId(request);
        Long filialId = tenantScopeService.filialId(request);
        return politicaRepository.findByEmpresaIdAndFilialIdOrderByDominioOperacaoAsc(empresaId, filialId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PoliticaSegregacaoResponseDTO salvarPolitica(PoliticaSegregacaoRequestDTO dto, HttpServletRequest request) {
        tenantFeatureGuardService.requireFeature(request, ApexFeature.ADVANCED_FINANCE,
                "Governanca", "configurar politica de segregacao de funcoes");
        authorizationService.require(request, ApexPermission.BUDGET_APPROVE,
                "Governanca", "configurar politica de segregacao de funcoes");
        if (!authorizationService.isOwnerOrSysadmin(request)) {
            throw new AccessDeniedException(
                    "Apenas proprietario (DONO/DONO_GERENTE) ou SYSADMIN pode alterar politicas de segregacao e alcada.");
        }

        Long empresaId = tenantScopeService.empresaId(request);
        Long filialId = tenantScopeService.filialId(request);
        String dominio = normalizeDomain(dto.dominioOperacao());

        PoliticaSegregacaoAprovacao politica = politicaRepository
                .findByEmpresaIdAndFilialIdAndDominioOperacao(empresaId, filialId, dominio)
                .orElseGet(PoliticaSegregacaoAprovacao::new);

        String anterior = politica.getIdPoliticaSod() == null ? null
                : "distinto=" + politica.isExigirAprovadorDistinto()
                        + ";unipessoal=" + politica.isPermitirExcecaoUnipessoal()
                        + ";alcadaGerente=" + politica.getAlcadaGerenteMaximo();

        politica.setEmpresaId(empresaId);
        politica.setFilialId(filialId);
        politica.setDominioOperacao(dominio);
        politica.setExigirAprovadorDistinto(dto.exigirAprovadorDistinto() == null || dto.exigirAprovadorDistinto());
        politica.setPermitirExcecaoUnipessoal(Boolean.TRUE.equals(dto.permitirExcecaoUnipessoal()));
        politica.setExigirJustificativa(dto.exigirJustificativa() == null || dto.exigirJustificativa());
        politica.setAlcadaGerenteMaximo(dto.alcadaGerenteMaximo());
        politica.setAtualizadoPor(privacyGuardService.currentUserLogin(request));

        PoliticaSegregacaoAprovacao saved = politicaRepository.save(politica);
        String novo = "distinto=" + saved.isExigirAprovadorDistinto()
                + ";unipessoal=" + saved.isPermitirExcecaoUnipessoal()
                + ";alcadaGerente=" + saved.getAlcadaGerenteMaximo();
        auditTrailService.registrarPrivacidade(
                "CONFIGURAR_POLITICA_SOD",
                "PoliticaSegregacaoAprovacao",
                saved.getIdPoliticaSod(),
                "Governanca",
                anterior,
                novo,
                request,
                "Atualizacao de politica SoD para dominio " + dominio + ".");
        return toDto(saved);
    }

    @Transactional
    public ValidacaoSegregacaoResponseDTO validarAprovacaoEndpoint(
            ValidacaoSegregacaoRequestDTO dto,
            HttpServletRequest request) {
        tenantFeatureGuardService.requireFeature(request, ApexFeature.ADVANCED_FINANCE,
                "Governanca", "validar segregacao de funcoes para aprovacao");
        String dominio = normalizeDomain(dto.dominioOperacao());
        ApexPermission requiredPermission = requiredPermissionForDomain(dominio);
        authorizationService.require(request, requiredPermission,
                "Governanca", "validar aprovacao no dominio " + dominio);
        return validarAprovacaoInterna(
                dominio,
                dto.submetidoPorUsuarioId(),
                dto.valorOperacao(),
                dto.justificativa(),
                request);
    }

    @Transactional
    public ValidacaoSegregacaoResponseDTO validarAprovacaoInterna(
            String dominioRaw,
            Long submetidoPorUsuarioId,
            BigDecimal valorOperacao,
            String justificativa,
            HttpServletRequest request) {
        String dominio = normalizeDomain(dominioRaw);
        if (submetidoPorUsuarioId == null || submetidoPorUsuarioId <= 0) {
            throw new IllegalArgumentException("Identificador do usuario solicitante e obrigatorio para validacao SoD.");
        }

        Long empresaId = tenantScopeService.empresaId(request);
        Long filialId = tenantScopeService.filialId(request);
        Long aprovadoPorUsuarioId = privacyGuardService.currentUserId(request);
        if (aprovadoPorUsuarioId == null || aprovadoPorUsuarioId <= 0) {
            throw new SegregationOfDutiesViolationException(
                    dominio,
                    "Identidade autenticada do aprovador ausente no token JWT para verificacao de segregacao.");
        }

        PoliticaSegregacaoAprovacao politica = politicaRepository
                .findByEmpresaIdAndFilialIdAndDominioOperacao(empresaId, filialId, dominio)
                .orElse(null);

        usuarioRepository.findById(submetidoPorUsuarioId).ifPresent(solicitante -> {
            if (solicitante.getEmpresaId() != null && !empresaId.equals(solicitante.getEmpresaId())) {
                auditTrailService.registrarPrivacidade(
                        "VIOLACAO_CROSS_TENANT_SOD",
                        "Usuario",
                        submetidoPorUsuarioId,
                        "Governanca",
                        null,
                        "empresaToken=" + empresaId + ";empresaSolicitante=" + solicitante.getEmpresaId(),
                        request,
                        "Tentativa bloqueada de validar aprovacao SoD com usuario de outro tenant.");
                throw new AccessDeniedException("Usuario solicitante pertence a outra empresa.");
            }
        });

        boolean exigirAprovadorDistinto = politica == null || politica.isExigirAprovadorDistinto();
        boolean permitirExcecaoUnipessoal = DOMINIO_LGPD_ANONIMIZACAO.equals(dominio)
                ? (politica == null || politica.isPermitirExcecaoUnipessoal())
                : (politica != null && politica.isPermitirExcecaoUnipessoal());
        boolean exigirJustificativa = politica == null || politica.isExigirJustificativa();
        BigDecimal alcadaGerente = politica != null ? politica.getAlcadaGerenteMaximo() : null;

        boolean ownerOrSysadmin = authorizationService.isOwnerOrSysadmin(request);
        long aprovadoresAtivos = contarAprovadoresElegiveisAtivos(empresaId, dominio);

        if (!ownerOrSysadmin && valorOperacao != null && alcadaGerente != null
                && valorOperacao.compareTo(alcadaGerente) > 0) {
            auditTrailService.registrarPrivacidade(
                    "VIOLACAO_ALCADA_GERENTE",
                    "PoliticaSegregacaoAprovacao",
                    politica != null ? politica.getIdPoliticaSod() : null,
                    "Governanca",
                    "alcadaMax=" + alcadaGerente,
                    "valorOperacao=" + valorOperacao + ";aprovadorUid=" + aprovadoPorUsuarioId,
                    request,
                    "Tentativa de aprovacao acima da alcada gerencial configurada.");
            throw new SegregationOfDutiesViolationException(
                    dominio,
                    "Valor da operacao (" + valorOperacao + ") excede a alcada maxima do gerente ("
                            + alcadaGerente + "). Requer aprovacao de DONO ou SYSADMIN.");
        }

        boolean mesmaIdentidade = aprovadoPorUsuarioId.equals(submetidoPorUsuarioId);
        boolean autoaprovacaoUnipessoal = false;

        if (mesmaIdentidade && exigirAprovadorDistinto) {
            boolean justificativaValida = isJustificationValid(justificativa);
            if (permitirExcecaoUnipessoal && aprovadoresAtivos <= 1 && ownerOrSysadmin) {
                if (!justificativaValida) {
                    auditTrailService.registrarPrivacidade(
                            "VIOLACAO_SOD_JUSTIFICATIVA",
                            "PoliticaSegregacaoAprovacao",
                            politica != null ? politica.getIdPoliticaSod() : null,
                            "Governanca",
                            null,
                            "dominio=" + dominio + ";uid=" + aprovadoPorUsuarioId,
                            request,
                            "Excecao unipessoal sem justificativa minima obrigatoria.");
                    throw new SegregationOfDutiesViolationException(
                            dominio,
                            "Excecao unipessoal de SoD exige justificativa formal com no minimo 10 caracteres.");
                }
                autoaprovacaoUnipessoal = true;
            } else {
                auditTrailService.registrarPrivacidade(
                        "VIOLACAO_SOD_AUTOAPROVACAO",
                        "PoliticaSegregacaoAprovacao",
                        politica != null ? politica.getIdPoliticaSod() : null,
                        "Governanca",
                        "submetidoPorUid=" + submetidoPorUsuarioId,
                        "aprovadorUid=" + aprovadoPorUsuarioId + ";elegiveis=" + aprovadoresAtivos
                                + ";unipessoalPermitido=" + permitirExcecaoUnipessoal,
                        request,
                        "Autoaprovacao bloqueada pela politica de segregacao de funcoes.");
                throw new SegregationOfDutiesViolationException(
                        dominio,
                        "Segregacao de funcoes violada: o aprovador (uid=" + aprovadoPorUsuarioId
                                + ") deve ser distinto do solicitante (uid=" + submetidoPorUsuarioId + ").");
            }
        }

        if (exigirJustificativa && !isJustificationValid(justificativa)) {
            auditTrailService.registrarPrivacidade(
                    "VIOLACAO_SOD_JUSTIFICATIVA",
                    "PoliticaSegregacaoAprovacao",
                    politica != null ? politica.getIdPoliticaSod() : null,
                    "Governanca",
                    null,
                    "dominio=" + dominio + ";aprovadorUid=" + aprovadoPorUsuarioId,
                    request,
                    "Aprovacao bloqueada por ausencia de justificativa obrigatoria.");
            throw new SegregationOfDutiesViolationException(
                    dominio,
                    "Justificativa formal obrigatoria (minimo 10 caracteres) para concluir a aprovacao.");
        }

        String acaoAudit = autoaprovacaoUnipessoal ? "APROVACAO_SOD_UNIPESSOAL" : "APROVACAO_SOD_VALIDADA";
        String parecer = autoaprovacaoUnipessoal
                ? "Autoaprovacao excepcional unipessoal autorizada para proprietario unico com trilha auditada."
                : "Aprovacao validada com segregacao de funcoes conforme politica do tenant.";

        auditTrailService.registrarPrivacidade(
                acaoAudit,
                "PoliticaSegregacaoAprovacao",
                politica != null ? politica.getIdPoliticaSod() : null,
                "Governanca",
                "submetidoPorUid=" + submetidoPorUsuarioId,
                "aprovadorUid=" + aprovadoPorUsuarioId + ";dominio=" + dominio
                        + ";unipessoal=" + autoaprovacaoUnipessoal,
                request,
                justificativa != null ? justificativa.trim() : parecer);

        return new ValidacaoSegregacaoResponseDTO(
                true,
                dominio,
                submetidoPorUsuarioId,
                aprovadoPorUsuarioId,
                autoaprovacaoUnipessoal,
                aprovadoresAtivos,
                valorOperacao,
                ownerOrSysadmin ? null : alcadaGerente,
                parecer);
    }

    public long contarAprovadoresElegiveisAtivos(Long empresaId, String dominio) {
        if (empresaId == null) {
            return 0;
        }
        String normalized = normalizeDomain(dominio);
        List<Usuario> ativos = usuarioRepository.findByEmpresaIdAndStatus(empresaId, 1);
        return ativos.stream()
                .filter(usuario -> usuario.getPerfil() != null && usuario.getPerfil().getNome() != null)
                .filter(usuario -> isEligibleApproverRole(usuario.getPerfil().getNome(), normalized))
                .count();
    }

    private boolean isEligibleApproverRole(String roleName, String dominio) {
        if (DOMINIO_LGPD_ANONIMIZACAO.equals(dominio)) {
            return authorizationService.isOwnerOrSysadminRole(roleName);
        }
        return authorizationService.roleHasPermission(roleName, requiredPermissionForDomain(dominio));
    }

    private ApexPermission requiredPermissionForDomain(String dominio) {
        if (DOMINIO_LGPD_ANONIMIZACAO.equals(dominio)) {
            return ApexPermission.PERSONAL_DATA_ANONYMIZE;
        }
        if (DOMINIO_INVESTIMENTO.equals(dominio)) {
            return ApexPermission.INVESTMENT_APPROVE;
        }
        return ApexPermission.BUDGET_APPROVE;
    }

    private boolean isJustificationValid(String justificativa) {
        return justificativa != null && justificativa.trim().length() >= MIN_JUSTIFICATION_LENGTH;
    }

    private String normalizeDomain(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Dominio de operacao SoD e obrigatorio.");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 48) {
            throw new IllegalArgumentException("Dominio de operacao SoD excede 48 caracteres.");
        }
        return normalized;
    }

    private PoliticaSegregacaoResponseDTO toDto(PoliticaSegregacaoAprovacao entity) {
        return new PoliticaSegregacaoResponseDTO(
                entity.getIdPoliticaSod(),
                entity.getEmpresaId(),
                entity.getFilialId(),
                entity.getDominioOperacao(),
                entity.isExigirAprovadorDistinto(),
                entity.isPermitirExcecaoUnipessoal(),
                entity.isExigirJustificativa(),
                entity.getAlcadaGerenteMaximo(),
                entity.getAtualizadoPor(),
                entity.getAtualizadoEm(),
                entity.getVersion());
    }
}
