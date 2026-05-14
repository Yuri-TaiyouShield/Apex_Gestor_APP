package Repository;

import Model.FinancialCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialCalculationRepository extends JpaRepository<FinancialCalculation, Long> {
}
