package Repository;

import Model.PrivacyRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyRequestRepository extends JpaRepository<PrivacyRequest, Long> {
}
