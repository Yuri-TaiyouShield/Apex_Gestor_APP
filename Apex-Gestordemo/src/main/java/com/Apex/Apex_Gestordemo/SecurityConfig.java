package com.Apex.Apex_Gestordemo;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final com.Apex.Apex_Gestordemo.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(com.Apex.Apex_Gestordemo.security.JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    private static final String[] ADMIN_ROLES = {
            "SYSADMIN", "DONO_GERENTE", "DONO", "ADMIN_MASTER", "ADMIN", "ADMINISTRACAO", "GESTOR", "GERENTE"
    };
    private static final String[] MANAGER_ROLES = {
            "SYSADMIN", "DONO_GERENTE", "DONO", "ADMIN_MASTER", "ADMIN", "ADMINISTRACAO", "GESTOR", "GERENTE", "SUPERVISOR"
    };
    private static final String[] SALES_ROLES = {
            "VENDEDOR", "CAIXA", "SUPERVISOR", "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER",
            "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] CASHIER_ROLES = {
            "CAIXA", "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER", "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] CASHIER_VIEW_ROLES = {
            "CAIXA", "FINANCEIRO", "FINANCAS", "AUDITOR", "CONTADOR",
            "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER", "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] STOCK_ROLES = {
            "ESTOQUISTA", "SUPERVISOR", "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER",
            "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] NF_ROLES = {
            "ESTOQUISTA", "DESPACHANTE", "FINANCEIRO", "FINANCAS",
            "SUPERVISOR", "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER", "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] FINANCE_ROLES = {
            "FINANCEIRO", "FINANCAS", "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER",
            "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] FINANCE_CLASSIFICATION_ROLES = {
            "FINANCEIRO", "FINANCAS", "CONTADOR", "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER",
            "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] FINANCE_READ_ROLES = {
            "FINANCEIRO", "FINANCAS", "AUDITOR", "CONTADOR",
            "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER", "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] FINANCE_COMPLIANCE_ROLES = {
            "FINANCEIRO", "FINANCAS", "AUDITOR", "CONTADOR", "ADVOGADO",
            "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO", "ADMIN_MASTER", "DONO", "DONO_GERENTE", "SYSADMIN"
    };
    private static final String[] PRIVACY_OWNER_ROLES = {
            "SYSADMIN", "DONO_GERENTE", "DONO"
    };
    private static final String[] AUDIT_VIEW_ROLES = {
            "SYSADMIN", "DONO_GERENTE", "DONO", "ADMIN_MASTER", "ADMIN", "ADMINISTRACAO",
            "GESTOR", "GERENTE", "AUDITOR", "FINANCEIRO", "FINANCAS"
    };
    private static final String[] STAFF_READ_ROLES = {
            "VENDEDOR", "CAIXA", "FINANCEIRO", "FINANCAS", "ESTOQUISTA", "DESPACHANTE",
            "AUDITOR", "CONTADOR", "ADVOGADO", "SUPERVISOR", "GESTOR", "GERENTE", "ADMIN", "ADMINISTRACAO",
            "ADMIN_MASTER", "DONO", "DONO_GERENTE", "SYSADMIN"
    };

    @Value("${apex.security.require-auth:true}")
    private boolean requireAuthentication;

    @Value("${apex.security.cors.allowed-origins:http://localhost,https://localhost,http://localhost:*,https://localhost:*,http://127.0.0.1,https://127.0.0.1,http://127.0.0.1:*,https://127.0.0.1:*,app://localhost,ionic://localhost,capacitor://localhost}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Acesso negado para esta operacao.\"}");
                        }))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)));
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (!requireAuthentication) {
            throw new IllegalStateException("Autenticacao obrigatoria: defina APEX_SECURITY_REQUIRE_AUTH=true apenas em ambientes explicitamente locais.");
        }
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/privacy/consents",
                                "/api/licenses/validate").permitAll()
                        .requestMatchers("/api/privacy/admin/**", "/api/privacy/requests", "/api/privacy/export/**",
                                "/api/privacy/clientes/**")
                        .hasAnyRole(PRIVACY_OWNER_ROLES)
                        .requestMatchers("/api/financeiro/documentos/**")
                        .hasAnyRole(FINANCE_COMPLIANCE_ROLES)
                        .requestMatchers("/api/financeiro/**")
                        .hasAnyRole(FINANCE_READ_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/formaspagamento/**", "/api/condicoes-pagamento/**",
                                "/api/categorias/**", "/api/grupos-produtos/**")
                        .hasAnyRole(STAFF_READ_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/despesas/**", "/api/relatorios/**")
                        .hasAnyRole(FINANCE_READ_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/despesas/tipos")
                        .hasAnyRole(FINANCE_CLASSIFICATION_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/despesas/tipos/**")
                        .hasAnyRole(FINANCE_CLASSIFICATION_ROLES)
                        .requestMatchers("/api/despesas/**", "/api/relatorios/**", "/api/admcalc/**",
                                "/api/condicoes-pagamento/**")
                        .hasAnyRole(FINANCE_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/caixa/atual", "/api/caixa/historico")
                        .hasAnyRole(CASHIER_VIEW_ROLES)
                        .requestMatchers("/api/caixa/**")
                        .hasAnyRole(CASHIER_ROLES)
                        .requestMatchers("/api/grupos-produtos/**", "/api/categorias/**", "/api/formaspagamento/**")
                        .hasAnyRole(ADMIN_ROLES)
                        .requestMatchers("/api/usuarios/**", "/api/perfis/**", "/api/menus/**")
                        .hasAnyRole(ADMIN_ROLES)
                        .requestMatchers("/api/auditoria/**")
                        .hasAnyRole(AUDIT_VIEW_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/nfs/**")
                        .hasAnyRole(NF_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/nfs/**")
                        .hasAnyRole(NF_ROLES)
                        .requestMatchers("/api/estoque/**")
                        .hasAnyRole(STOCK_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/produtos/**")
                        .hasAnyRole(STOCK_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/produtos/**")
                        .hasAnyRole(STOCK_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/produtos/**")
                        .hasAnyRole(STOCK_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/vendas/*/cancelar")
                        .hasAnyRole(MANAGER_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/vendas/**")
                        .hasAnyRole(SALES_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/vendas/**")
                        .hasAnyRole(SALES_ROLES)
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(parseAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Idempotency-Key",
                "X-Requested-With",
                "X-Trace-Id",
                "X-Client-Platform",
                "X-Apex-License-Key",
                "X-Apex-Device-Fingerprint",
                "X-Apex-App-Id",
                "X-Apex-Tenant-Code"));
        configuration.setExposedHeaders(List.of("X-Trace-Id"));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
