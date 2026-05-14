package Repository;

import Model.FinancialAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialAuditEventRepository extends JpaRepository<FinancialAuditEvent, Long> {
}
