package Repository;

import Model.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {

    Optional<TenantSubscription> findFirstByTenantTenantCodeAndStatusOrderByStartedAtDesc(String tenantCode, String status);
}
