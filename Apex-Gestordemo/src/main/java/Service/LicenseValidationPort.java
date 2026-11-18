package Service;

import Model.EmpresaPlanoLicenca;
import com.Apex.Apex_Gestordemo.security.ApexFeature;

public interface LicenseValidationPort {

    RemoteLicenseCheckResult check(EmpresaPlanoLicenca plano, ApexFeature feature);

    record RemoteLicenseCheckResult(boolean valid, boolean transientFailure, String status, String message) {
        public static RemoteLicenseCheckResult ok() {
            return new RemoteLicenseCheckResult(true, false, "ATIVO", "Validacao externa confirmada.");
        }

        public static RemoteLicenseCheckResult denied(String status, String message) {
            return new RemoteLicenseCheckResult(false, false, status, message);
        }

        public static RemoteLicenseCheckResult transientError(String message) {
            return new RemoteLicenseCheckResult(false, true, "TRANSIENT_FAILURE", message);
        }
    }
}
