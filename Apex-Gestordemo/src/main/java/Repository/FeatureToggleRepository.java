package Repository;

import Model.FeatureToggle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureToggleRepository extends JpaRepository<FeatureToggle, Long> {

    Optional<FeatureToggle> findByFeatureKeyAndStatus(String featureKey, String status);

    List<FeatureToggle> findByStatus(String status);
}
