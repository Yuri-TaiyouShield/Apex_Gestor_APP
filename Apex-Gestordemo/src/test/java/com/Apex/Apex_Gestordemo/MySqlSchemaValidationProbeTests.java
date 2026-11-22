package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIf("isMySqlAvailable")
@SpringBootTest(properties = {
        "spring.datasource.url=${DB_URL:jdbc:mysql://127.0.0.1:3306/apex_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:apex_dev_2026}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration-mysql",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "apex.jwt.secret=mysql-schema-probe-secret-with-32-chars",
        "apex.security.require-auth=true"
})
class MySqlSchemaValidationProbeTests {

    static boolean isMySqlAvailable() {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 3306), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mysqlAplicaFlywayEValidaContratoJpa() {
        Integer migrations = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = 1
                  AND version IN ('2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17')
                """, Integer.class);
        Integer auditRevision = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'audit_revision'
                """, Integer.class);

        assertThat(migrations).isEqualTo(16);
        assertThat(auditRevision).isEqualTo(1);
    }
}
