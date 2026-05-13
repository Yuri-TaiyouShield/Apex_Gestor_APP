package Repository;

import Model.DataErasureLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataErasureLogRepository extends JpaRepository<DataErasureLog, Long> {
}
