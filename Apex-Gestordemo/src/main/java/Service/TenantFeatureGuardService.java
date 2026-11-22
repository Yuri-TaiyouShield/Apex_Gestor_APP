package Service;

import DTO.TenantEntitlementResponseDTO;
import Model.EmpresaPlanoLicenca;
import Repository.EmpresaPlanoLicencaRepository;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.FeatureNotLicensedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TenantFeatureGuardService {

    private static final Set<String> HARD_BLOCKED_STATUSES = Set.of(
            "SUSPENSO", "CANCELADO", "BLOQUEADO", "REVOGADO", "INATIVO");
    private static final Set<String> CONTINGENCY_STATUSES = Set.of(
            "CONTINGENCIA", "GRACE_PERIOD", "TRANSIENT_FAILURE");

    private final EmpresaPlanoLicencaRepository empresaPlanoLicencaRepository;
    private final TenantScopeService tenantScopeService;
    private final LicenseService licenseService;
    private final AuditTrailService auditTrailService;

    @Autowired(required = false)
    private LicenseValidationPort licenseValidationPort;

    @Value("${apex.license.grace-period-hours:72}")
    private int defaultGracePeriodHours = 72;

    @Autowired
    public TenantFeatureGuardService(
            EmpresaPlanoLicencaRepository empresaPlanoLicencaRepository,
            TenantScopeService tenantScopeService,
            LicenseService licenseService,
            AuditTrailService auditTrailService) {
        this.empresaPlanoLicencaRepository = empresaPlanoLicencaRepository;
        this.tenantScopeService = tenantScopeService;
        this.licenseService = licenseService;
        this.auditTrailService = auditTrailService;
    }

    public void requireFeature(HttpServletRequest request, ApexFeature feature) {
        requireFeature(request, feature, "Licenciamento", "acessar funcionalidade " + feature.name());
    }

    public void requireFeature(HttpServletRequest request, ApexFeature feature, String module, String action) {
        Long empresaId = tenantScopeService.empresaId(request);
        EvaluationResult result = evaluate(empresaId, feature, LocalDateTime.now());
        if (result.allowed()) {
            if (result.degradedMode()) {
                audit(request, "CONTINGENCIA_LICENCA_ATIVA", module, empresaId,
                        "feature=" + feature.name() + ";acao=" + action + ";contingenciaAte=" + result.contingenciaAte(),
                        "Acesso liberado dentro da janela limitada de contingencia.");
            }
            return;
        }
        audit(request, "ACESSO_FEATURE_NEGADO", module, empresaId,
                "feature=" + feature.name() + ";status=" + result.status() + ";acao=" + action,
                result.message());
        throw new FeatureNotLicensedException(feature, result.status(), result.message());
    }

    public boolean hasFeature(HttpServletRequest request, ApexFeature feature) {
        Long empresaId = tenantScopeService.empresaId(request);
        return evaluate(empresaId, feature, LocalDateTime.now()).allowed();
    }

    public boolean hasFeatureForEmpresa(Long empresaId, ApexFeature feature) {
        return empresaId != null && evaluate(empresaId, feature, LocalDateTime.now()).allowed();
    }

    public TenantEntitlementResponseDTO resolveEntitlement(Long empresaId) {
        return licenseService.getEntitlement(empresaId);
    }

    private EvaluationResult evaluate(Long empresaId, ApexFeature feature, LocalDateTime now) {
        EmpresaPlanoLicenca plano = empresaPlanoLicencaRepository.findByEmpresaId(empresaId).orElse(null);
        if (plano == null) {
            return evaluateLegacyCompatibility(feature);
        }

        String status = normalizeStatus(plano.getStatus());
        if (HARD_BLOCKED_STATUSES.contains(status)) {
            return EvaluationResult.denied(status,
                    "Licenca comercial da empresa encontra-se " + status.toLowerCase(Locale.ROOT) + ".");
        }

        List<String> features = licenseService.parseFeaturesCsv(
                plano.getFeaturesHabilitadas(), plano.getSubscriptionTier());
        if (!features.contains(feature.name())) {
            return EvaluationResult.denied("PLAN_UPGRADE_REQUIRED",
                    "Funcionalidade " + feature.name() + " nao contratada para o plano atual ("
                            + plano.getSubscriptionTier() + ").");
        }

        if ("EXPIRADO".equals(status)) {
            return EvaluationResult.denied("EXPIRADO",
                    "Assinatura expirada para a empresa. Renove a licenca para utilizar " + feature.name() + ".");
        }

        if (licenseValidationPort != null) {
            EvaluationResult remoteCheck = evaluateRemotePort(plano, feature, now, status);
            if (remoteCheck != null) {
                return remoteCheck;
            }
        }

        if (CONTINGENCY_STATUSES.contains(status)) {
            return evaluateContingencyWindow(plano, now,
                    "Janela de contingencia da licenca expirada. Revalide online para continuar.");
        }

        if (plano.getValidoAte() != null && plano.getValidoAte().isBefore(now)) {
            return EvaluationResult.denied("EXPIRADO",
                    "Vigencia da licenca expirada em " + plano.getValidoAte() + ".");
        }

        return EvaluationResult.ok(status, plano.getContingenciaAte());
    }

    private EvaluationResult evaluateLegacyCompatibility(ApexFeature feature) {
        if (feature == ApexFeature.FINANCIAL_CORE || feature == ApexFeature.BASIC_PRICING) {
            return EvaluationResult.ok("COMPAT_ESSENTIAL", null);
        }
        return EvaluationResult.denied("NO_ACTIVE_PLAN",
                "Funcionalidade " + feature.name() + " exige plano licenciado ativo com contratacao especifica.");
    }

    private EvaluationResult evaluateRemotePort(
            EmpresaPlanoLicenca plano,
            ApexFeature feature,
            LocalDateTime now,
            String localStatus) {
        try {
            LicenseValidationPort.RemoteLicenseCheckResult check = licenseValidationPort.check(plano, feature);
            if (check == null || check.valid()) {
                refreshRemoteTimestamp(plano, now);
                return null;
            }
            if (!check.transientFailure()) {
                return EvaluationResult.denied(
                        check.status() != null ? check.status() : "LICENSE_REVOKED",
                        check.message() != null ? check.message() : "Licenca invalida pela validacao remota.");
            }
            return evaluateContingencyWindow(plano, now,
                    "Falha transiente na validacao da licenca e periodo de contingencia esgotado.");
        } catch (RuntimeException ex) {
            if ("EXPIRADO".equals(localStatus)) {
                return EvaluationResult.denied("EXPIRADO", "Licenca expirada.");
            }
            return evaluateContingencyWindow(plano, now,
                    "Falha transiente na validacao da licenca e periodo de contingencia esgotado.");
        }
    }

    private void refreshRemoteTimestamp(EmpresaPlanoLicenca plano, LocalDateTime now) {
        if (plano.getValidoAte() != null && plano.getValidoAte().isBefore(now)) {
            return;
        }
        if (plano.getUltimaValidacaoEm() != null && !plano.getUltimaValidacaoEm().isBefore(now.minusMinutes(5))) {
            return;
        }
        int graceHours = resolveGraceHours(plano);
        plano.setUltimaValidacaoEm(now);
        plano.setContingenciaAte(now.plusHours(graceHours));
        try {
            empresaPlanoLicencaRepository.save(plano);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ignored) {
            // Atualizacao concorrente de timestamp remoto durante leituras simultaneas nao deve falhar a requisicao
        }
    }

    private EvaluationResult evaluateContingencyWindow(
            EmpresaPlanoLicenca plano,
            LocalDateTime now,
            String expiredMessage) {
        if (plano.getValidoAte() != null && plano.getValidoAte().isBefore(now)) {
            return EvaluationResult.denied("EXPIRADO", "Licenca comercial expirada.");
        }
        int graceHours = resolveGraceHours(plano);
        if (graceHours <= 0) {
            return EvaluationResult.denied("GRACE_PERIOD_DISABLED", expiredMessage);
        }
        LocalDateTime deadline = resolveContingencyDeadline(plano, graceHours);
        if (deadline != null && !now.isAfter(deadline)) {
            return EvaluationResult.contingency("GRACE_PERIOD", deadline);
        }
        return EvaluationResult.denied("GRACE_PERIOD_EXPIRED", expiredMessage);
    }

    private LocalDateTime resolveContingencyDeadline(EmpresaPlanoLicenca plano, int graceHours) {
        LocalDateTime fromLastValidation = plano.getUltimaValidacaoEm() != null
                ? plano.getUltimaValidacaoEm().plusHours(graceHours)
                : null;
        LocalDateTime storedContingency = plano.getContingenciaAte();
        if (fromLastValidation != null && storedContingency != null) {
            return fromLastValidation.isBefore(storedContingency) ? fromLastValidation : storedContingency;
        }
        if (fromLastValidation != null) {
            return fromLastValidation;
        }
        return storedContingency;
    }

    private int resolveGraceHours(EmpresaPlanoLicenca plano) {
        return plano.getGracePeriodHoras() != null
                ? Math.max(0, plano.getGracePeriodHoras())
                : Math.max(0, defaultGracePeriodHours);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ATIVO";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private void audit(
            HttpServletRequest request,
            String acao,
            String module,
            Long empresaId,
            String valorNovo,
            String justificativa) {
        if (auditTrailService == null) {
            return;
        }
        auditTrailService.registrarPrivacidade(
                acao,
                "EmpresaPlanoLicenca",
                empresaId,
                module,
                null,
                valorNovo,
                request,
                justificativa);
    }

    private record EvaluationResult(
            boolean allowed,
            boolean degradedMode,
            String status,
            String message,
            LocalDateTime contingenciaAte) {
        private static EvaluationResult ok(String status, LocalDateTime contingenciaAte) {
            return new EvaluationResult(true, false, status, null, contingenciaAte);
        }

        private static EvaluationResult contingency(String status, LocalDateTime contingenciaAte) {
            return new EvaluationResult(true, true, status, null, contingenciaAte);
        }

        private static EvaluationResult denied(String status, String message) {
            return new EvaluationResult(false, false, status, message, null);
        }
    }
}
