package Repository;

import Model.FinancialDocumentDeliveryOutbox;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialDocumentDeliveryOutboxRepository extends JpaRepository<FinancialDocumentDeliveryOutbox, Long> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<FinancialDocumentDeliveryOutbox> findById(Long id);

    List<FinancialDocumentDeliveryOutbox> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByQueuedAtAsc(
            List<String> statuses, LocalDateTime now);
}
