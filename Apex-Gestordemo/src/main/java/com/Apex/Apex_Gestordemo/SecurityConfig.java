package com.Apex.Apex_Gestordemo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import Service.LicenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${apex.security.jwt.secret}")
    private String jwtSecret;

    @Value("${apex.security.require-https:false}")
    private boolean requireHttps;

    @Bean
    @Order(1)
    SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/health", "/api/auth/**", "/api/privacy/consents", "/api/licenses/**", "/media/catalog/**")
                .csrf(csrf -> csrf.ignoringRequestMatchers("/actuator/health", "/api/auth/**", "/api/privacy/consents", "/api/licenses/**", "/media/catalog/**"))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)));

        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain protectedSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/produtos", "/api/produtos/**", "/api/categorias", "/api/categorias/**", "/media/catalog/**").permitAll()
                        .requestMatchers("/api/privacy/**").authenticated()
                        .requestMatchers("/api/usuarios/**", "/api/perfis/**", "/api/menus/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "ADMIN", "GERENTE", "GESTOR")
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/enriquecimento/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "FINANCEIRO", "ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/b2c/cart/merge").hasAnyRole("CLIENTE_B2C", "SYSADMIN", "DONO_GERENTE", "ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/vendas").hasAnyRole("CLIENTE_B2C", "DONO_GERENTE", "VENDEDOR", "CAIXA", "ADMIN", "GERENTE")
                        .requestMatchers("/api/vendas/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "FINANCEIRO", "VENDEDOR", "CAIXA", "DESPACHANTE", "ADMIN", "GERENTE")
                        .requestMatchers("/api/clientes/**").hasAnyRole("DONO_GERENTE", "VENDEDOR", "CAIXA", "ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/produtos/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "FINANCEIRO", "ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/produtos/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "FINANCEIRO", "ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.PATCH, "/api/produtos/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "FINANCEIRO", "ADMIN", "GERENTE")
                        .requestMatchers("/api/fornecedores/**", "/api/nfs/**").hasAnyRole("DONO_GERENTE", "FINANCEIRO", "ADMIN", "GERENTE")
                        .requestMatchers("/api/despesas/**", "/api/relatorios/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "FINANCEIRO", "ADMIN", "GERENTE", "GESTOR", "FINANCAS", "ADMINISTRACAO", "AUDITOR")
                        .requestMatchers("/api/financeiro/**").hasAnyRole("SYSADMIN", "DONO_GERENTE", "ADMINISTRACAO", "FINANCEIRO", "FINANCAS", "GERENTE", "GESTOR", "AUDITOR", "CONTADOR", "ADVOGADO")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)));

        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    @Bean
    FilterRegistrationBean<LicenseBootstrapFilter> licenseBootstrapFilter(LicenseService licenseService) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FilterRegistrationBean<LicenseBootstrapFilter> registration = new FilterRegistrationBean<>(
                new LicenseBootstrapFilter(licenseService, objectMapper)
        );
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/api/licenses/validate");
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecret.getBytes(StandardCharsets.UTF_8)));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://localhost:*",
                "https://*.app.github.dev",
                "capacitor://localhost",
                "ionic://localhost",
                "app://localhost"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Client-Platform",
                "X-Apex-License-Key",
                "X-Apex-Device-Fingerprint",
                "X-Apex-App-Id",
                "X-Apex-Tenant-Code"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.<GrantedAuthority>of();
            }
            return roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return converter;
    }

    private SecretKey jwtSecretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
