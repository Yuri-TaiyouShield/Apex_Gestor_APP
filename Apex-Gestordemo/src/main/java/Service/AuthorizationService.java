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
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.PAYMENT_CONDITION_MANAGE,
                ApexPermission.SETTINGS_PERSONAL_MANAGE,
                ApexPermission.USER_VIEW,
                ApexPermission.PERSONAL_DATA_VIEW,
                ApexPermission.AUDIT_LOG_VIEW));
        ROLE_PERMISSIONS.put(Role.CAIXA, EnumSet.of(
                ApexPermission.PRODUCT_VIEW,
                ApexPermission.CUSTOMER_VIEW,
                ApexPermission.STOCK_VIEW,
                ApexPermission.SALE_CREATE,
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
                ApexPermission.SALE_DISCOUNT_APPLY,
                ApexPermission.PAYMENT_CONDITION_VIEW,
                ApexPermission.SETTINGS_PERSONAL_MANAGE));
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

    private static Set<ApexPermission> withoutSysadminOnly(EnumSet<ApexPermission> permissions) {
        permissions.remove(ApexPermission.PERMISSION_MANAGE);
        return permissions;
    }

    private Role resolve(String role) {
        return switch (role) {
            case "ROLE_SYSADMIN" -> Role.SYSADMIN;
            case "ROLE_DONO_GERENTE", "ROLE_DONO" -> Role.DONO;
            case "ROLE_ADMIN", "ROLE_ADMIN_MASTER", "ROLE_GERENTE", "ROLE_GESTOR", "ROLE_SUPERVISOR" -> Role.GERENTE;
            case "ROLE_CAIXA" -> Role.CAIXA;
            case "ROLE_VENDEDOR" -> Role.VENDEDOR;
            case "ROLE_CLIENTE_B2C" -> Role.CLIENTE;
            default -> Role.PUBLICO;
        };
    }

    private enum Role {
        SYSADMIN,
        DONO,
        GERENTE,
        CAIXA,
        VENDEDOR,
        CLIENTE,
        PUBLICO
    }
}
