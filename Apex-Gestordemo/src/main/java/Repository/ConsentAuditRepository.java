package Repository;

import Model.ConsentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsentAuditRepository extends JpaRepository<ConsentAudit, Long> {
    List<ConsentAudit> findByTitularId(String titularId);
}
