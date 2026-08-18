package com.Apex.Apex_Gestordemo;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final com.Apex.Apex_Gestordemo.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(com.Apex.Apex_Gestordemo.security.JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    private static final String[] ADMIN_ROLES = {"SYSADMIN", "DONO_GERENTE", "ADMIN_MASTER", "ADMIN", "GESTOR", "GERENTE"};
    private static final String[] MANAGER_ROLES = {"SYSADMIN", "DONO_GERENTE", "ADMIN_MASTER", "ADMIN", "GESTOR", "GERENTE", "SUPERVISOR"};
    private static final String[] SALES_ROLES = {"VENDEDOR", "CAIXA", "SUPERVISOR", "GESTOR", "GERENTE", "ADMIN", "DONO_GERENTE", "SYSADMIN"};
    private static final String[] CASHIER_ROLES = {"CAIXA", "GESTOR", "GERENTE", "ADMIN", "DONO_GERENTE", "SYSADMIN"};
    private static final String[] STOCK_ROLES = {"ESTOQUISTA", "SUPERVISOR", "GESTOR", "GERENTE", "ADMIN", "DONO_GERENTE", "SYSADMIN"};
    private static final String[] FINANCE_ROLES = {"FINANCEIRO", "GESTOR", "GERENTE", "ADMIN", "DONO_GERENTE", "SYSADMIN"};

    @Value("${apex.security.require-auth:false}")
    private boolean requireAuthentication;

    @Value("${apex.security.cors.allowed-origins:http://localhost:*,https://localhost:*,http://127.0.0.1:*,https://127.0.0.1:*,app://localhost,ionic://localhost,capacitor://localhost}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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

        if (requireAuthentication) {
            http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/auth/**", "/api/privacy/consents").permitAll()
                            .requestMatchers("/api/privacy/admin/**", "/api/privacy/requests", "/api/privacy/export/**",
                                    "/api/privacy/clientes/**")
                            .hasAnyRole(ADMIN_ROLES)
                            .requestMatchers("/api/despesas/**", "/api/relatorios/**", "/api/admcalc/**",
                                    "/api/condicoes-pagamento/**")
                            .hasAnyRole(FINANCE_ROLES)
                            .requestMatchers("/api/caixa/**")
                            .hasAnyRole(CASHIER_ROLES)
                            .requestMatchers("/api/grupos-produtos/**", "/api/categorias/**", "/api/formaspagamento/**")
                            .hasAnyRole(ADMIN_ROLES)
                            .requestMatchers("/api/usuarios/**", "/api/perfis/**", "/api/menus/**")
                            .hasAnyRole(ADMIN_ROLES)
                            .requestMatchers("/api/auditoria/**")
                            .hasAnyRole(ADMIN_ROLES)
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
                            .anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

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
                "X-Requested-With",
                "X-Trace-Id",
                "X-Client-Platform",
                "X-Apex-License-Key",
                "X-Apex-Device-Fingerprint",
                "X-Apex-App-Id",
                "X-Apex-Tenant-Code",
                "X-Apex-User-Id",
                "X-Apex-User-Login",
                "X-Apex-Roles",
                "X-Apex-Empresa-Id",
                "X-Apex-Filial-Id"));
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
