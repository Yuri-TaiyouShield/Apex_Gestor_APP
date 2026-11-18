package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import Service.JwtTokenService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("local")
@SpringBootTest(properties = {
        "apex.seed.homologacao=true",
        "apex.security.require-auth=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "apex.jwt.secret=mysql-integration-test-secret-32-chars",
        "apex.jwt.issuer=apex-gestor-test",
        "apex.jwt.audience=apex-api-test"
})
@AutoConfigureMockMvc
class MySqlOperationalFlowTests {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("apex_test")
            .withUsername("apex")
            .withPassword("apex_test_only");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService jwtTokenService;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void mysqlValidaFlywayTenantRbacVendaCaixaFinanceiroEstoqueDevolucaoELgpd() throws Exception {
        Integer migrations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version IN ('2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17')",
                Integer.class);
        assertThat(migrations).isEqualTo(16);

        mvc.perform(get("/api/caixa/atual"))
                .andExpect(status().isUnauthorized());
        String expiredToken = jwtTokenService.issueAccessToken(6L, "Vendedor Homologacao", "v5_vendedor",
                List.of("ROLE_VENDEDOR"), 1L, 1L, Duration.ofSeconds(-1));
        mvc.perform(get("/api/caixa/atual").header("Authorization", bearer(expiredToken)))
                .andExpect(status().isUnauthorized());

        String seller = login("v5_vendedor");
        String cashier = login("v5_caixa");
        String manager = login("v5_gerente");

        mvc.perform(get("/api/caixa/atual").header("Authorization", bearer(seller)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/produtos").header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/financeiro/operacional/contas-receber").header("Authorization", bearer(seller)))
                .andExpect(status().isForbidden());

        Long productId = id("SELECT id_produto FROM produtos WHERE codigo_barras='7890000001001'");
        Long categoryId = id("SELECT categoria_id FROM produtos WHERE id_produto=" + productId);
        jdbc.update("""
                INSERT INTO produtos (descricao,custo,codigo_barras,codigo_interno,quantidade_estoque,estoque_minimo,
                  valor_venda,status,categoria_id,empresa_id,filial_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """, "PRODUTO FORA DO TENANT", new BigDecimal("1.00"), "7890000099999", "TENANT-2", 9, 1,
                new BigDecimal("2.00"), 1, categoryId, 2L, 2L);
        mvc.perform(get("/api/produtos").header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigoBarras == '7890000099999')]").isEmpty());

        Long clientId = id("SELECT id_cliente FROM cliente WHERE empresa_id=1 AND filial_id=1 ORDER BY id_cliente LIMIT 1");
        Long conditionId = id("SELECT id_condicao_pagamento FROM condicoes_pagamento WHERE nome='A vista' AND empresa_id=1 LIMIT 1");
        Long cashId = id("SELECT id_forma_pagamento FROM formas_pagamento WHERE nome='Dinheiro' AND empresa_id=1 LIMIT 1");
        Long pixId = id("SELECT id_forma_pagamento FROM formas_pagamento WHERE nome='PIX' AND empresa_id=1 LIMIT 1");
        Integer stockBefore = jdbc.queryForObject("SELECT quantidade_estoque FROM produtos WHERE id_produto=?", Integer.class, productId);

        String saleBody = json.writeValueAsString(Map.of(
                "clienteId", clientId, "desconto", 0, "condicaoPagamentoId", conditionId,
                "itens", new Object[] { Map.of("produtoId", productId, "quantidade", 2) },
                "pagamentos", new Object[] {}));
        String saleResponse = mvc.perform(post("/api/vendas").header("Authorization", bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON).content(saleBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CAIXA"))
                .andReturn().getResponse().getContentAsString();
        JsonNode sale = json.readTree(saleResponse);
        long saleId = sale.get("idVenda").asLong();
        BigDecimal total = sale.get("valorTotal").decimalValue();
        assertThat(jdbc.queryForObject("SELECT quantidade_estoque FROM produtos WHERE id_produto=?", Integer.class, productId))
                .isEqualTo(stockBefore);

        mvc.perform(post("/api/caixa/abrir").header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":100.00,\"observacao\":\"Teste MySQL\"}"))
                .andExpect(status().isOk());

        BigDecimal cashPart = new BigDecimal("20.00");
        String receiveBody = json.writeValueAsString(Map.of("pagamentos", new Object[] {
                Map.of("formaPagamentoId", cashId, "valor", cashPart, "numeroParcelas", 1),
                Map.of("formaPagamentoId", pixId, "valor", total.subtract(cashPart), "numeroParcelas", 1)
        }));
        mvc.perform(post("/api/caixa/vendas/{id}/receber", saleId).header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content(receiveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.pagamentos.length()").value(2));

        assertThat(jdbc.queryForObject("SELECT quantidade_estoque FROM produtos WHERE id_produto=?", Integer.class, productId))
                .isEqualTo(stockBefore - 2);
        assertThat(jdbc.queryForObject("SELECT status FROM conta_receber WHERE venda_id=?", String.class, saleId))
                .isEqualTo("PAGO");
        assertThat(jdbc.queryForObject("SELECT saldo FROM conta_receber WHERE venda_id=?", BigDecimal.class, saleId))
                .isEqualByComparingTo(BigDecimal.ZERO);

        String invalidReturn = json.writeValueAsString(Map.of("tipo", "TOTAL", "motivo", "",
                "itens", new Object[] { Map.of("produtoOriginalId", productId, "quantidade", 1) }));
        mvc.perform(post("/api/vendas/{id}/devolucoes", saleId).header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content(invalidReturn))
                .andExpect(status().isBadRequest());
        String validReturn = json.writeValueAsString(Map.of("tipo", "PARCIAL", "motivo", "Homologação Testcontainers",
                "itens", new Object[] { Map.of("produtoOriginalId", productId, "quantidade", 1) }));
        mvc.perform(post("/api/vendas/{id}/devolucoes", saleId).header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content(validReturn))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credito_cliente WHERE venda_origem_id=? AND saldo>0", Integer.class, saleId))
                .isEqualTo(1);

        Long creditId = id("SELECT id_credito FROM credito_cliente WHERE venda_origem_id=" + saleId + " LIMIT 1");
        Long creditMethodId = id("SELECT id_forma_pagamento FROM formas_pagamento WHERE nome='Credito/Vale' AND empresa_id=1 LIMIT 1");
        String creditSaleBody = json.writeValueAsString(Map.of(
                "clienteId", clientId, "desconto", 0, "condicaoPagamentoId", conditionId,
                "itens", new Object[] { Map.of("produtoId", productId, "quantidade", 1) },
                "pagamentos", new Object[] {}));
        String creditSaleResponse = mvc.perform(post("/api/vendas").header("Authorization", bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON).content(creditSaleBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CAIXA"))
                .andReturn().getResponse().getContentAsString();
        JsonNode creditSale = json.readTree(creditSaleResponse);
        long creditSaleId = creditSale.get("idVenda").asLong();
        BigDecimal creditSaleTotal = creditSale.get("valorTotal").decimalValue();
        String creditReceiveBody = json.writeValueAsString(Map.of("pagamentos", new Object[] {
                Map.of("formaPagamentoId", creditMethodId, "valor", creditSaleTotal,
                        "numeroParcelas", 1, "creditoId", creditId)
        }));
        mvc.perform(post("/api/caixa/vendas/{id}/receber", creditSaleId).header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content(creditReceiveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.pagamentos[0].creditoId").value(creditId));
        assertThat(jdbc.queryForObject("SELECT status FROM credito_cliente WHERE id_credito=?", String.class, creditId))
                .isEqualTo("UTILIZADO");
        assertThat(jdbc.queryForObject("SELECT saldo FROM credito_cliente WHERE id_credito=?", BigDecimal.class, creditId))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM credito_movimentacao WHERE credito_id=? AND venda_id=? AND tipo='USO'",
                Integer.class, creditId, creditSaleId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_log WHERE empresa_id=1 AND filial_id=1", Integer.class))
                .isGreaterThan(0);

        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"idle\"}"))
                .andExpect(status().isNoContent());
        Map<String, Object> logoutAudit = jdbc.queryForMap("""
                SELECT usuario_login, valor_novo, justificativa
                  FROM auditoria_log
                 WHERE acao='LOGOUT' AND entidade='SESSAO'
                 ORDER BY id_auditoria DESC LIMIT 1
                """);
        assertThat(logoutAudit.get("usuario_login")).isEqualTo("v5_gerente");
        assertThat(logoutAudit.get("valor_novo").toString()).isEqualTo("reason=INATIVIDADE");
        assertThat(logoutAudit.toString()).doesNotContain("Bearer", "refresh", "accessToken", "Apex@2026");

        String clients = mvc.perform(get("/api/clientes").header("Authorization", bearer(seller)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(clients).doesNotContain("99999999999");
        String users = mvc.perform(get("/api/usuarios").header("Authorization", bearer(manager)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(users).doesNotContain("senha", "hash", "refreshToken", "accessToken");
    }

    private String login(String login) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "senha", "Apex@2026",
                                "acceptedPrivacyTerms", true, "consentVersion", "v1"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asText();
    }

    private Long id(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private String bearer(String token) { return "Bearer " + token; }
}
