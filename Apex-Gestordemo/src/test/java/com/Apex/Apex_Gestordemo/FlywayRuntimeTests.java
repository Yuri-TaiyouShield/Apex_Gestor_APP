package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:apex_flyway_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=update"
})
class FlywayRuntimeTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayExecutaMigrationVersionadaNoRuntime() {
        Integer historico = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'flyway_schema_history'
                """, Integer.class);
        Integer caixaSessao = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'Caixa_Sessao'
                """, Integer.class);
        Integer condicoesPagamento = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'Condicoes_Pagamento'
                """, Integer.class);
        Integer categoriaEmpresa = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'Categoria'
                  AND COLUMN_NAME = 'empresa_id'
                """, Integer.class);
        Integer produtoCodigoBarras = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'Produto'
                  AND COLUMN_NAME = 'codigo_barras'
                """, Integer.class);
        Integer produtoBarcodeIndex = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.INDEXES
                WHERE TABLE_NAME = 'Produto'
                  AND INDEX_NAME = 'idx_produto_tenant_codigo_barras'
                """, Integer.class);
        Integer vendaStatusOperacional = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'Venda' AND COLUMN_NAME = 'status_operacional'
                """, Integer.class);
        Integer contaReceber = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Conta_Receber'
                """, Integer.class);
        Integer devolucao = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Venda_Devolucao'
                """, Integer.class);
        Integer credito = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Credito_Cliente'
                """, Integer.class);
        Integer saldoReceber = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'Conta_Receber' AND COLUMN_NAME = 'saldo'
                """, Integer.class);

        assertThat(historico).isEqualTo(1);
        assertThat(caixaSessao).isEqualTo(1);
        assertThat(condicoesPagamento).isEqualTo(1);
        assertThat(categoriaEmpresa).isEqualTo(1);
        assertThat(produtoCodigoBarras).isEqualTo(1);
        assertThat(produtoBarcodeIndex).isGreaterThanOrEqualTo(1);
        assertThat(vendaStatusOperacional).isEqualTo(1);
        assertThat(contaReceber).isEqualTo(1);
        assertThat(devolucao).isEqualTo(1);
        assertThat(credito).isEqualTo(1);
        assertThat(saldoReceber).isEqualTo(1);
    }
}
