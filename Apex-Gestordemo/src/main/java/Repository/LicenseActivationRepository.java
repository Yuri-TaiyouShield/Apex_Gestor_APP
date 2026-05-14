package Repository;

import Model.LicenseActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseActivationRepository extends JpaRepository<LicenseActivation, Long> {

    Optional<LicenseActivation> findByLicenseKeyHashAndDeviceHashAndAppId(String licenseKeyHash, String deviceHash, String appId);

    long countByLicenseKeyHashAndStatus(String licenseKeyHash, String status);

    List<LicenseActivation> findByLicenseKeyHashAndStatus(String licenseKeyHash, String status);
}
