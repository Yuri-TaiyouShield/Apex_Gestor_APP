package Repository;

import Model.LicenseActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicenseActivationRepository extends JpaRepository<LicenseActivation, Long> {

    Optional<LicenseActivation> findByLicenseKeyHashAndDeviceHash(String licenseKeyHash, String deviceHash);

    long countByLicenseKeyHashAndStatus(String licenseKeyHash, String status);
}
