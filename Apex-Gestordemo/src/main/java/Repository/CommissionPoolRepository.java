package Repository;

import Model.CommissionPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionPoolRepository extends JpaRepository<CommissionPool, Long> {

    Optional<CommissionPool> findByTenantTenantCodeAndOrderIdAndStatus(String tenantCode, String orderId, String status);

    List<CommissionPool> findByTenantTenantCodeAndStatus(String tenantCode, String status);
}
