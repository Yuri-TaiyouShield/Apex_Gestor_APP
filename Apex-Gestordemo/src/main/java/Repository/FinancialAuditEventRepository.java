package Repository;

import Model.FinancialAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialAuditEventRepository extends JpaRepository<FinancialAuditEvent, Long> {
    Page<FinancialAuditEvent> findByEmpresaIdAndFilialId(Long empresaId, Long filialId, Pageable pageable);
}
