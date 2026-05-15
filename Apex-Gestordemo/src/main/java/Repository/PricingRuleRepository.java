package Repository;

import Model.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    Optional<PricingRule> findFirstByTenantTenantCodeAndRuleTypeAndStatusOrderByEffectiveFromDesc(String tenantCode, String ruleType, String status);
}
