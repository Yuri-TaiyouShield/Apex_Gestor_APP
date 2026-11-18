package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Model.Perfil;
import Model.Usuario;
import Repository.PerfilRepository;
import Repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "apex.seed.homologacao=true",
        "apex.security.require-auth=true"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class AuthLoginAndRbacFlowTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void loginTodosOsCincoPerfisHomologacaoRetorna200ERolesCorretas() throws Exception {
        Map<String, String> expectedRoles = new LinkedHashMap<>();
        expectedRoles.put("v5_dono", "ROLE_DONO_GERENTE");
        expectedRoles.put("v5_sysadmin", "ROLE_SYSADMIN");
        expectedRoles.put("v5_gerente", "ROLE_GERENTE");
        expectedRoles.put("v5_caixa", "ROLE_CAIXA");
        expectedRoles.put("v5_vendedor", "ROLE_VENDEDOR");

        for (Map.Entry<String, String> entry : expectedRoles.entrySet()) {
            JsonNode auth = loginOk(entry.getKey(), "Apex@2026");
            assertThat(auth.get("accessToken").asText()).isNotBlank();
            assertThat(auth.get("refreshToken").asText()).isNotBlank();
            assertThat(auth.get("login").asText()).isEqualTo(entry.getKey());
            assertThat(auth.get("roles")).extracting(JsonNode::asText).containsExactly(entry.getValue());
        }

        assertThat(usuarioRepository.findByLogin("v5_cliente")).isEmpty();
    }

    @Test
    void loginComSenhaErradaUsuarioInexistenteEPerfilClienteB2c() throws Exception {
        performLogin("v5_caixa", "senha-incorreta")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"))
                .andExpect(jsonPath("$.detail").value("Credenciais invalidas ou sessao expirada."));

        performLogin("v5_cliente", "Apex@2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"))
                .andExpect(jsonPath("$.detail").value("Credenciais invalidas ou sessao expirada."));

        Usuario caixa = usuarioRepository.findByLogin("v5_caixa").orElseThrow();
        Perfil perfilCliente = perfilRepository.findByNome("CLIENTE_B2C").orElseGet(() -> {
            Perfil p = new Perfil();
            p.setNome("CLIENTE_B2C");
            p.setStatus(1);
            return perfilRepository.save(p);
        });

        Usuario clienteB2c = new Usuario();
        clienteB2c.setNome("Cliente B2C Teste");
        clienteB2c.setLogin("cliente_b2c_auth_test");
        clienteB2c.setSenha(passwordEncoder.encode("Apex@2026"));
        clienteB2c.setDataNascimento(LocalDate.of(1995, 5, 20));
        clienteB2c.setStatus(1);
        clienteB2c.setEmpresaId(caixa.getEmpresaId());
        clienteB2c.setFilialId(caixa.getFilialId());
        clienteB2c.setPerfil(perfilCliente);
        clienteB2c = usuarioRepository.save(clienteB2c);

        try {
            performLogin("cliente_b2c_auth_test", "senha-incorreta")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Nao autenticado"))
                    .andExpect(jsonPath("$.detail").value("Credenciais invalidas ou sessao expirada."));

            performLogin("cliente_b2c_auth_test", "Apex@2026")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Acesso negado"))
                    .andExpect(jsonPath("$.detail").value("Acesso de login para perfil cliente foi desabilitado."));
        } finally {
            usuarioRepository.deleteById(clienteB2c.getIdUsuario());
        }
    }

    @Test
    void caixaAutenticadoAcessaRotasPermitidasERecebe403EmRotasRestritas() throws Exception {
        String token = loginOk("v5_caixa", "Apex@2026").get("accessToken").asText();

        MvcResult caixaAtual = mvc.perform(get("/api/caixa/atual").header("Authorization", bearer(token))).andReturn();
        assertThat(caixaAtual.getResponse().getStatus()).isIn(200, 204);

        for (String path : new String[] {
                "/api/caixa/vendas-pendentes",
                "/api/formaspagamento",
                "/api/condicoes-pagamento",
                "/api/categorias",
                "/api/grupos-produtos",
                "/api/produtos",
                "/api/clientes",
                "/api/vendas/historico"
        }) {
            mvc.perform(get(path).header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
        }

        for (String path : new String[] {
                "/api/relatorios/financeiro",
                "/api/usuarios"
        }) {
            mvc.perform(get(path).header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }

        for (String path : new String[] {
                "/api/formaspagamento",
                "/api/condicoes-pagamento",
                "/api/categorias",
                "/api/grupos-produtos",
                "/api/produtos"
        }) {
            mvc.perform(post(path).header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void vendedorAutenticadoAcessaDashboardECondicoesPagamentoERecebe403NoCaixa() throws Exception {
        String token = loginOk("v5_vendedor", "Apex@2026").get("accessToken").asText();

        mvc.perform(get("/api/dashboard/vendedor").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/condicoes-pagamento").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/caixa/atual").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    private JsonNode loginOk(String login, String senha) throws Exception {
        String body = performLogin(login, senha)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body);
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(String login, String senha) throws Exception {
        String payload = json.writeValueAsString(Map.of(
                "login", login,
                "senha", senha,
                "consentVersion", "2026-01",
                "acceptedPrivacyTerms", true));
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
