package com.Apex.Apex_Gestordemo;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Model.Notificacao;
import Model.Usuario;
import Repository.NotificacaoRepository;
import Repository.UsuarioRepository;
import Service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "apex.seed.homologacao=true",
        "apex.security.require-auth=true"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class NotificacaoCorsTests {

    private static final List<String> TRUSTED_ORIGINS = List.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200",
            "app://localhost",
            "https://localhost",
            "capacitor://localhost",
            "ionic://localhost"
    );

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void preflightAndAuthenticatedGetAllowConfiguredWebElectronAndCapacitorOrigins() throws Exception {
        String token = loginAndExtractToken("v5_vendedor", "Apex@2026");

        for (String origin : TRUSTED_ORIGINS) {
            mvc.perform(options("/api/notificacoes/pendentes")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));

            mvc.perform(get("/api/notificacoes/pendentes")
                            .header("Origin", origin)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void rejectsUntrustedOriginWithoutEchoingCorsHeaders() throws Exception {
        String token = loginAndExtractToken("v5_vendedor", "Apex@2026");
        String untrusted = "https://evil.example.com";

        mvc.perform(options("/api/notificacoes/pendentes")
                        .header("Origin", untrusted)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mvc.perform(get("/api/notificacoes/pendentes")
                        .header("Origin", untrusted)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void preservesAuthenticationAndTenantIsolationOnPendingNotifications() throws Exception {
        mvc.perform(get("/api/notificacoes/pendentes")
                        .header("Origin", "app://localhost"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "app://localhost"));

        Usuario vendedor = usuarioRepository.findByLogin("v5_vendedor").orElseThrow();
        Long ownEmpresa = vendedor.getEmpresaId() != null ? vendedor.getEmpresaId() : 1L;
        Long ownFilial = vendedor.getFilialId() != null ? vendedor.getFilialId() : 1L;
        Long foreignEmpresa = ownEmpresa + 900L;
        Long otherFilial = ownFilial + 10L;

        Notificacao ownNotif = new Notificacao();
        ownNotif.setTipo("ALERTA_TESTE");
        ownNotif.setTitulo("Notificação do Tenant");
        ownNotif.setMensagem("Mensagem restrita à empresa ativa.");
        ownNotif.setStatus("PENDENTE");
        ownNotif.setEmpresaId(ownEmpresa);
        ownNotif.setFilialId(ownFilial);

        Notificacao foreignNotif = new Notificacao();
        foreignNotif.setTipo("ALERTA_TESTE");
        foreignNotif.setTitulo("Notificação de Outra Empresa");
        foreignNotif.setMensagem("Não deve vazar entre empresas.");
        foreignNotif.setStatus("PENDENTE");
        foreignNotif.setEmpresaId(foreignEmpresa);
        foreignNotif.setFilialId(ownFilial);

        Notificacao otherFilialNotif = new Notificacao();
        otherFilialNotif.setTipo("ALERTA_TESTE");
        otherFilialNotif.setTitulo("Notificação de Outra Filial");
        otherFilialNotif.setMensagem("Não deve vazar entre filiais.");
        otherFilialNotif.setStatus("PENDENTE");
        otherFilialNotif.setEmpresaId(ownEmpresa);
        otherFilialNotif.setFilialId(otherFilial);

        Notificacao savedOwn = notificacaoRepository.save(ownNotif);
        Notificacao savedForeign = notificacaoRepository.save(foreignNotif);
        Notificacao savedOtherFilial = notificacaoRepository.save(otherFilialNotif);

        try {
            String token = loginAndExtractToken("v5_vendedor", "Apex@2026");

            mvc.perform(get("/api/notificacoes/pendentes")
                            .header("Origin", "https://localhost")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "https://localhost"))
                    .andExpect(jsonPath("$[*].titulo", hasItem("Notificação do Tenant")))
                    .andExpect(jsonPath("$[*].titulo", not(hasItem("Notificação de Outra Empresa"))))
                    .andExpect(jsonPath("$[*].titulo", not(hasItem("Notificação de Outra Filial"))))
                    .andExpect(jsonPath("$[0].idNotificacao", not(nullValue())));

            String advogadoToken = jwtTokenService.issueAccessToken(
                    999L,
                    "Advogado Teste",
                    "advogado_cors_teste",
                    List.of("ROLE_ADVOGADO"),
                    ownEmpresa,
                    ownFilial,
                    Duration.ofMinutes(5));

            mvc.perform(get("/api/notificacoes/pendentes")
                            .header("Origin", "https://localhost")
                            .header("Authorization", "Bearer " + advogadoToken))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("Access-Control-Allow-Origin", "https://localhost"));
        } finally {
            notificacaoRepository.deleteAllById(List.of(
                    savedOwn.getIdNotificacao(),
                    savedForeign.getIdNotificacao(),
                    savedOtherFilial.getIdNotificacao()));
        }
    }

    private String loginAndExtractToken(String login, String senha) throws Exception {
        String payload = json.writeValueAsString(Map.of(
                "login", login,
                "senha", senha,
                "consentVersion", "2026-01",
                "acceptedPrivacyTerms", true));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }
}
