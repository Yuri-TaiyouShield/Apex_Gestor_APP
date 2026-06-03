package Controller;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2) && hasRequiredTables(connection)) {
                return ResponseEntity.ok(Map.of(
                        "status", "UP",
                        "components", Map.of(
                                "database", Map.of("status", "UP"),
                                "schema", Map.of("status", "UP")
                        )
                ));
            }
        } catch (Exception ignored) {
            // Keep health output small and do not leak infrastructure details.
        }
        return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "components", Map.of(
                        "database", Map.of("status", "DOWN"),
                        "schema", Map.of("status", "DOWN")
                )
        ));
    }

    private boolean hasRequiredTables(Connection connection) {
        String[] checks = {
                "SELECT 1 FROM usuario LIMIT 1",
                "SELECT 1 FROM produtos LIMIT 1",
                "SELECT 1 FROM license_activation LIMIT 1"
        };
        for (String sql : checks) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (Exception exception) {
                return false;
            }
        }
        return true;
    }
}
