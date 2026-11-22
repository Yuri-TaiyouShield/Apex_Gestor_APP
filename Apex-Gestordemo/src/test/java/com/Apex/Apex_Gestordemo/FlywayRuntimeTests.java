package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:apex_flyway_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "apex.jwt.secret=flyway-runtime-test-secret-with-32-chars",
        "apex.security.require-auth=true"
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
        Integer migrations = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                  AND version IN ('2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17')
                """, Integer.class);
        Integer caixaSessao = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'caixa_sessao'
                """, Integer.class);
        Integer condicoesPagamento = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'condicoes_pagamento'
                """, Integer.class);
        Integer categoriaEmpresa = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'categoria'
                  AND COLUMN_NAME = 'empresa_id'
                """, Integer.class);
        Integer produtoCodigoBarras = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'produto'
                  AND COLUMN_NAME = 'codigo_barras'
                """, Integer.class);
        Integer produtoBarcodeIndex = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.INDEXES
                WHERE TABLE_NAME = 'produto'
                  AND INDEX_NAME = 'idx_produto_tenant_codigo_barras'
                """, Integer.class);
        Integer vendaStatusOperacional = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'venda' AND COLUMN_NAME = 'status_operacional'
                """, Integer.class);
        Integer contaReceber = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'conta_receber'
                """, Integer.class);
        Integer devolucao = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'venda_devolucao'
                """, Integer.class);
        Integer credito = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'credito_cliente'
                """, Integer.class);
        Integer saldoReceber = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'conta_receber' AND COLUMN_NAME = 'saldo'
                """, Integer.class);
        Integer empresaPlanoLicenca = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'empresa_plano_licenca'
                """, Integer.class);
        Integer politicaSod = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'politica_segregacao_aprovacao'
                """, Integer.class);

        assertThat(historico).isEqualTo(1);
        assertThat(migrations).isEqualTo(16);
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
        assertThat(empresaPlanoLicenca).isEqualTo(1);
        assertThat(politicaSod).isEqualTo(1);
    }
}
