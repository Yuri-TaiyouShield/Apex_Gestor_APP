package Repository;

import Model.FinancialDocumentDeliveryOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialDocumentDeliveryOutboxRepository extends JpaRepository<FinancialDocumentDeliveryOutbox, Long> {
}
