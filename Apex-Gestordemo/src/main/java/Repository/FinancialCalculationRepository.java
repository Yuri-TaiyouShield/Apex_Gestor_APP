package Repository;

import Model.FinancialCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialCalculationRepository extends JpaRepository<FinancialCalculation, Long> {
    Page<FinancialCalculation> findByEmpresaIdAndFilialId(Long empresaId, Long filialId, Pageable pageable);
}
