package Repository;

import Model.FinancialDigitalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialDigitalDocumentRepository extends JpaRepository<FinancialDigitalDocument, Long> {
}
