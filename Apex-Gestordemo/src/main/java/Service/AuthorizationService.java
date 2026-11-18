package Service;

import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private static final Map<Role, Set<ApexPermission>> ROLE_PERMISSIONS = new EnumMap<>(Role.class);

    static {
        ROLE_PERMISSIONS.put(Role.SYSADMIN, EnumSet.allOf(ApexPermission.class));
        ROLE_PERMISSIONS.put(Role.DONO, withoutSysadminOnly(EnumSet.allOf(ApexPermission.class)));
        ROLE_PERMISSIONS.put(Role.GERENTE, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.PRODUCT_CREATE,
                ApexPermission.PRODUCT_UPDATE,
                ApexPermission.PRODUCT_DISABLE,
                ApexPermission.PRODUCT_PRICE_UPDATE,
                ApexPermission.CUSTOMER_VIEW,
                ApexPermission.CUSTOMER_CREATE,
                ApexPermission.CUSTOMER_UPDATE,
                ApexPermission.CUSTOMER_DISABLE,
                ApexPermission.SUPPLIER_VIEW,
                ApexPermission.SUPPLIER_CREATE,
                ApexPermission.SUPPLIER_UPDATE,
                ApexPermission.SUPPLIER_DISABLE,
                ApexPermission.STOCK_VIEW,
                ApexPermission.STOCK_ADJUST,
                ApexPermission.STOCK_CHANGE,
                ApexPermission.STOCK_TRANSFER,
                ApexPermission.SALE_CREATE,
                ApexPermission.SALE_RECEIVE,
                ApexPermission.SALE_HISTORY_VIEW,
                ApexPermission.SALE_RETURN,
                ApexPermission.SALE_CANCEL,
                ApexPermission.SALE_DISCOUNT_APPLY,
                ApexPermission.SALE_DISCOUNT_OVERRIDE,
                ApexPermission.DISCOUNT_APPROVE,
                ApexPermission.CASHIER_VIEW,
                ApexPermission.CASHIER_OPEN,
                ApexPermission.CASHIER_SUPPLY,
                ApexPermission.CASHIER_WITHDRAW,
                ApexPermission.CASHIER_CLOSE,
                ApexPermission.CASH_CLOSE,
                ApexPermission.FINANCE_VIEW,
                ApexPermission.FINANCE_MANAGE,
                ApexPermission.FINANCE_CHANGE,
                ApexPermission.FINANCE_DRE_VIEW,
                ApexPermission.FINANCE_CASHFLOW_VIEW,
                ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                ApexPermission.FINANCE_COMPLIANCE_VIEW,
                ApexPermission.PAYABLE_SETTLE,
                ApexPermission.RECEIVABLE_SETTLE,
                ApexPermission.PRICING_MANAGE,
                ApexPermission.BUDGET_VIEW,
                ApexPermission.BUDGET_MANAGE,
                ApexPermission.BUDGET_APPROVE,
                ApexPermission.FORECAST_VIEW,
                ApexPermission.FORECAST_MANAGE,
                ApexPermission.GOALS_VIEW,
                ApexPermission.GOALS_MANAGE,
                ApexPermission.SCENARIO_SIMULATE,
                ApexPermission.INVESTMENT_VIEW,
                ApexPermission.INVESTMENT_MANAGE,
                ApexPermission.INVESTMENT_APPROVE,
                ApexPermission.ADMIN_DIAGNOSTICS_VIEW,
                ApexPermission.ADMIN_CONSULTANT_USE,
                ApexPermission.REPORT_EXPORT,
                ApexPermission.FINANCIAL_INTELLIGENCE_VIEW,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.PAYMENT_CONDITION_MANAGE,
                ApexPermission.SETTINGS_PERSONAL_MANAGE,
                ApexPermission.ROLE_MANAGE,
                ApexPermission.USER_VIEW,
                ApexPermission.PERSONAL_DATA_VIEW,
                ApexPermission.AUDIT_LOG_VIEW));
        // ADMIN_MASTER: administrador tecnico sem acesso irrestrito a dados pessoais ou operacao financeira livre
        ROLE_PERMISSIONS.put(Role.ADMIN_MASTER, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.SUPPLIER_VIEW,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.SETTINGS_GLOBAL_MANAGE,
                ApexPermission.SETTINGS_PERSONAL_MANAGE,
                ApexPermission.ROLE_MANAGE,
                ApexPermission.USER_VIEW,
                ApexPermission.USER_MANAGE,
                ApexPermission.AUDIT_LOG_VIEW,
                ApexPermission.FINANCE_VIEW,
                ApexPermission.FINANCE_DRE_VIEW,
                ApexPermission.FINANCE_CASHFLOW_VIEW,
                ApexPermission.FINANCIAL_INTELLIGENCE_VIEW,
                ApexPermission.FINANCE_COMPLIANCE_VIEW,
                ApexPermission.BUDGET_VIEW,
                ApexPermission.FORECAST_VIEW,
                ApexPermission.GOALS_VIEW,
                ApexPermission.INVESTMENT_VIEW,
                ApexPermission.ADMIN_DIAGNOSTICS_VIEW));
        ROLE_PERMISSIONS.put(Role.CAIXA, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.CUSTOMER_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.SALE_CREATE,
                ApexPermission.SALE_RECEIVE,
                ApexPermission.SALE_HISTORY_VIEW,
                ApexPermission.SALE_DISCOUNT_APPLY,
                ApexPermission.CASHIER_VIEW,
                ApexPermission.CASHIER_OPEN,
                ApexPermission.CASHIER_SUPPLY,
                ApexPermission.CASHIER_WITHDRAW,
                ApexPermission.CASHIER_CLOSE,
                ApexPermission.CASH_CLOSE,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.SETTINGS_PERSONAL_MANAGE));
        ROLE_PERMISSIONS.put(Role.VENDEDOR, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.CUSTOMER_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.SALE_CREATE,
                ApexPermission.SALE_HISTORY_VIEW,
                ApexPermission.SALE_DISCOUNT_APPLY,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.SETTINGS_PERSONAL_MANAGE));
        ROLE_PERMISSIONS.put(Role.FINANCEIRO, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.SUPPLIER_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.STOCK_CHANGE,
                ApexPermission.SALE_HISTORY_VIEW,
                ApexPermission.CASHIER_VIEW,
                ApexPermission.FINANCE_VIEW,
                ApexPermission.FINANCE_MANAGE,
                ApexPermission.FINANCE_CHANGE,
                ApexPermission.FINANCE_DRE_VIEW,
                ApexPermission.FINANCE_CASHFLOW_VIEW,
                ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                ApexPermission.FINANCE_COMPLIANCE_VIEW,
                ApexPermission.PAYABLE_SETTLE,
                ApexPermission.RECEIVABLE_SETTLE,
                ApexPermission.PRICING_MANAGE,
                ApexPermission.BUDGET_VIEW,
                ApexPermission.BUDGET_MANAGE,
                ApexPermission.FORECAST_VIEW,
                ApexPermission.FORECAST_MANAGE,
                ApexPermission.GOALS_VIEW,
                ApexPermission.SCENARIO_SIMULATE,
                ApexPermission.INVESTMENT_VIEW,
                ApexPermission.INVESTMENT_MANAGE,
                ApexPermission.ADMIN_DIAGNOSTICS_VIEW,
                ApexPermission.ADMIN_CONSULTANT_USE,
                ApexPermission.REPORT_EXPORT,
                ApexPermission.FINANCIAL_INTELLIGENCE_VIEW,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.PAYMENT_CONDITION_MANAGE,
                ApexPermission.PERSONAL_DATA_VIEW,
                ApexPermission.AUDIT_LOG_VIEW,
                ApexPermission.SETTINGS_PERSONAL_MANAGE));
        ROLE_PERMISSIONS.put(Role.AUDITOR, EnumSet.of(
                ApexPermission.FINANCE_VIEW,
                ApexPermission.FINANCE_DRE_VIEW,
                ApexPermission.FINANCE_CASHFLOW_VIEW,
                ApexPermission.FINANCIAL_INTELLIGENCE_VIEW,
                ApexPermission.FINANCE_COMPLIANCE_VIEW,
                ApexPermission.BUDGET_VIEW,
                ApexPermission.FORECAST_VIEW,
                ApexPermission.GOALS_VIEW,
                ApexPermission.INVESTMENT_VIEW,
                ApexPermission.ADMIN_DIAGNOSTICS_VIEW,
                ApexPermission.AUDIT_LOG_VIEW,
                ApexPermission.SALE_HISTORY_VIEW,
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.SUPPLIER_VIEW,
                ApexPermission.CUSTOMER_VIEW,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.CASHIER_VIEW,
                ApexPermission.PERSONAL_DATA_VIEW));
        ROLE_PERMISSIONS.put(Role.CONTADOR, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.SUPPLIER_VIEW,
                ApexPermission.CUSTOMER_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.SALE_HISTORY_VIEW,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.CASHIER_VIEW,
                ApexPermission.PERSONAL_DATA_VIEW,
                ApexPermission.FINANCE_VIEW,
                ApexPermission.FINANCE_DRE_VIEW,
                ApexPermission.FINANCE_CASHFLOW_VIEW,
                ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                ApexPermission.FINANCE_COMPLIANCE_VIEW,
                ApexPermission.FINANCE_COMPLIANCE_SIGN,
                ApexPermission.FINANCIAL_INTELLIGENCE_VIEW,
                ApexPermission.BUDGET_VIEW,
                ApexPermission.FORECAST_VIEW,
                ApexPermission.GOALS_VIEW,
                ApexPermission.INVESTMENT_VIEW,
                ApexPermission.ADMIN_DIAGNOSTICS_VIEW,
                ApexPermission.REPORT_EXPORT,
                ApexPermission.SETTINGS_PERSONAL_MANAGE));
        // ADVOGADO: restrito ao escopo juridico/compliance, sem leitura financeira generica (DRE/Caixa/Indicadores)
        ROLE_PERMISSIONS.put(Role.ADVOGADO, EnumSet.of(
                ApexPermission.FINANCE_COMPLIANCE_VIEW,
                ApexPermission.FINANCE_COMPLIANCE_SIGN,
                ApexPermission.SETTINGS_PERSONAL_MANAGE));
        ROLE_PERMISSIONS.put(Role.SUPERVISOR, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.PRODUCT_CREATE,
                ApexPermission.PRODUCT_UPDATE,
                ApexPermission.CUSTOMER_VIEW,
                ApexPermission.CUSTOMER_CREATE,
                ApexPermission.CUSTOMER_UPDATE,
                ApexPermission.SUPPLIER_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.STOCK_ADJUST,
                ApexPermission.STOCK_CHANGE,
                ApexPermission.STOCK_TRANSFER,
                ApexPermission.SALE_CREATE,
                ApexPermission.SALE_RECEIVE,
                ApexPermission.SALE_HISTORY_VIEW,
                ApexPermission.SALE_RETURN,
                ApexPermission.SALE_CANCEL,
                ApexPermission.SALE_DISCOUNT_APPLY,
                ApexPermission.DISCOUNT_APPROVE,
                ApexPermission.CASHIER_VIEW,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.SETTINGS_PERSONAL_MANAGE));
        ROLE_PERMISSIONS.put(Role.DESPACHANTE, EnumSet.of(
                ApexPermission.STOCK_VIEW,
                ApexPermission.STOCK_CHANGE,
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.SUPPLIER_VIEW));
        ROLE_PERMISSIONS.put(Role.ESTOQUISTA, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.STOCK_ADJUST,
                ApexPermission.STOCK_CHANGE,
                ApexPermission.STOCK_TRANSFER));
        ROLE_PERMISSIONS.put(Role.CLIENTE, EnumSet.of(ApexPermission.SETTINGS_PERSONAL_MANAGE));
        ROLE_PERMISSIONS.put(Role.PUBLICO, EnumSet.noneOf(ApexPermission.class));
    }

    @Autowired
    private PrivacyGuardService privacyGuardService;
    @Autowired
    private AuditTrailService auditTrailService;

    public boolean can(HttpServletRequest request, ApexPermission permission) {
        return currentPermissions(request).contains(permission);
    }

    public void require(HttpServletRequest request, ApexPermission permission, String module, String action) {
        if (!can(request, permission)) {
            auditTrailService.registrarPrivacidade(
                    "ACESSO_NEGADO",
                    "Permissao",
                    null,
                    module,
                    null,
                    "permissao=" + permission.getCode() + ";acao=" + action,
                    request,
                    "Tentativa bloqueada por RBAC backend.");
            throw new AccessDeniedException("Permissao insuficiente: " + permission.getCode());
        }
    }

    public Set<ApexPermission> currentPermissions(HttpServletRequest request) {
        EnumSet<ApexPermission> permissions = EnumSet.noneOf(ApexPermission.class);
        for (String role : privacyGuardService.currentRoles(request)) {
            permissions.addAll(ROLE_PERMISSIONS.getOrDefault(resolve(role), Set.of()));
        }
        return permissions;
    }

    public boolean isSysadmin(HttpServletRequest request) {
        return privacyGuardService.currentRoles(request).contains("ROLE_SYSADMIN");
    }

    public boolean isOwnerOrSysadmin(HttpServletRequest request) {
        return privacyGuardService.isPrivacyOwner(request);
    }

    public boolean roleHasPermission(String roleName, ApexPermission permission) {
        if (roleName == null || permission == null) {
            return false;
        }
        return ROLE_PERMISSIONS.getOrDefault(resolve(canonicalRole(roleName)), Set.of()).contains(permission);
    }

    public boolean isOwnerOrSysadminRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        Role resolved = resolve(canonicalRole(roleName));
        return resolved == Role.SYSADMIN || resolved == Role.DONO;
    }

    public void validatePerfilAssignment(HttpServletRequest request, String targetRoleName) {
        if (targetRoleName == null || targetRoleName.isBlank()) {
            return;
        }
        String canonical = canonicalRole(targetRoleName);
        Role target = resolveWithCompactFallback(canonical);
        if (target == Role.SYSADMIN && !isSysadmin(request)) {
            auditTrailService.registrarPrivacidade(
                    "ESCALACAO_PRIVILEGIO_BLOQUEADA",
                    "Perfil",
                    null,
                    "Usuarios",
                    null,
                    "targetRole=" + canonical,
                    request,
                    "Tentativa bloqueada de gerenciar ou atribuir perfil SYSADMIN.");
            throw new AccessDeniedException("Apenas SYSADMIN pode gerenciar ou atribuir o perfil SYSADMIN.");
        }
        if ((target == Role.DONO || target == Role.ADMIN_MASTER) && !isOwnerOrSysadmin(request)) {
            auditTrailService.registrarPrivacidade(
                    "ESCALACAO_PRIVILEGIO_BLOQUEADA",
                    "Perfil",
                    null,
                    "Usuarios",
                    null,
                    "targetRole=" + canonical,
                    request,
                    "Tentativa bloqueada de gerenciar ou atribuir perfil superior (" + canonical + ").");
            throw new AccessDeniedException(
                    "Apenas proprietario (DONO/DONO_GERENTE) ou SYSADMIN pode gerenciar ou atribuir o perfil "
                            + targetRoleName + ".");
        }
    }

    public void validateProtectedTargetUser(HttpServletRequest request, Model.Usuario targetUser) {
        if (targetUser != null && targetUser.getPerfil() != null && targetUser.getPerfil().getNome() != null) {
            validatePerfilAssignment(request, targetUser.getPerfil().getNome());
        }
    }

    public static String canonicalRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return "ROLE_PUBLICO";
        }
        String ascii = java.text.Normalizer.normalize(rawRole.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "_")
                .toUpperCase(java.util.Locale.ROOT);
        if (ascii.isBlank()) {
            return "ROLE_PUBLICO";
        }
        return ascii.startsWith("ROLE_") ? ascii : "ROLE_" + ascii;
    }

    private Role resolveWithCompactFallback(String canonical) {
        Role direct = resolve(canonical);
        if (direct != Role.PUBLICO) {
            return direct;
        }
        String compact = canonical.replaceFirst("^ROLE_", "").replace("_", "");
        return switch (compact) {
            case "SYSADMIN" -> Role.SYSADMIN;
            case "DONO", "DONOGERENTE" -> Role.DONO;
            case "ADMINMASTER" -> Role.ADMIN_MASTER;
            default -> Role.PUBLICO;
        };
    }

    private static Set<ApexPermission> withoutSysadminOnly(EnumSet<ApexPermission> permissions) {
        permissions.remove(ApexPermission.PERMISSION_MANAGE);
        return permissions;
    }

    private Role resolve(String role) {
        if (role == null) {
            return Role.PUBLICO;
        }
        return switch (canonicalRole(role)) {
            case "ROLE_SYSADMIN" -> Role.SYSADMIN;
            case "ROLE_DONO_GERENTE", "ROLE_DONO" -> Role.DONO;
            case "ROLE_ADMIN_MASTER" -> Role.ADMIN_MASTER;
            case "ROLE_ADMIN", "ROLE_ADMINISTRACAO", "ROLE_GERENTE", "ROLE_GESTOR" -> Role.GERENTE;
            case "ROLE_SUPERVISOR" -> Role.SUPERVISOR;
            case "ROLE_CAIXA" -> Role.CAIXA;
            case "ROLE_VENDEDOR" -> Role.VENDEDOR;
            case "ROLE_FINANCEIRO", "ROLE_FINANCAS" -> Role.FINANCEIRO;
            case "ROLE_AUDITOR" -> Role.AUDITOR;
            case "ROLE_CONTADOR" -> Role.CONTADOR;
            case "ROLE_ADVOGADO" -> Role.ADVOGADO;
            case "ROLE_DESPACHANTE" -> Role.DESPACHANTE;
            case "ROLE_ESTOQUISTA" -> Role.ESTOQUISTA;
            case "ROLE_CLIENTE_B2C", "ROLE_CLIENTE" -> Role.CLIENTE;
            default -> Role.PUBLICO;
        };
    }

    private enum Role {
        SYSADMIN,
        DONO,
        ADMIN_MASTER,
        GERENTE,
        SUPERVISOR,
        CAIXA,
        VENDEDOR,
        FINANCEIRO,
        AUDITOR,
        CONTADOR,
        ADVOGADO,
        DESPACHANTE,
        ESTOQUISTA,
        CLIENTE,
        PUBLICO
    }
}
