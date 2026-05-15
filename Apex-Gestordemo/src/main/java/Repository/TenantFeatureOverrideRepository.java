package Repository;

import Model.TenantFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantFeatureOverrideRepository extends JpaRepository<TenantFeatureOverride, Long> {

    List<TenantFeatureOverride> findByTenantTenantCode(String tenantCode);

    Optional<TenantFeatureOverride> findByTenantTenantCodeAndFeatureToggleFeatureKey(String tenantCode, String featureKey);
}
