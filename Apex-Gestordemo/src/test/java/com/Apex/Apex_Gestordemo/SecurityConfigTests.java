package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTests {

    @Test
    void passwordEncoderUsesAdaptiveHash() {
        PasswordEncoder encoder = new SecurityConfig(null).passwordEncoder();

        String encoded = encoder.encode("senha-forte-de-teste");

        assertThat(encoded).startsWith("$2");
        assertThat(encoder.matches("senha-forte-de-teste", encoded)).isTrue();
        assertThat(encoder.matches("senha-incorreta", encoded)).isFalse();
    }

    @Test
    void methodSecurityIsExplicitlyEnabled() {
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class)).isTrue();
    }

    @Test
    void prodCorsPolicySeparatesNativeOriginsFromLoopbackDevOrigins() throws Exception {
        Properties prodProps = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));
        String rawProp = prodProps.getProperty("apex.security.cors.allowed-origins");
        assertThat(rawProp).isNotBlank();
        String prodDefault = rawProp.replaceFirst("^\\$\\{APEX_CORS_ALLOWED_ORIGINS:", "").replaceFirst("\\}$", "");

        SecurityConfig config = new SecurityConfig(null);
        ReflectionTestUtils.setField(config, "allowedOrigins", prodDefault);
        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/notificacoes/pendentes"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isFalse();
        assertThat(cors.checkOrigin("https://localhost")).isEqualTo("https://localhost");
        assertThat(cors.checkOrigin("app://localhost")).isEqualTo("app://localhost");
        assertThat(cors.checkOrigin("ionic://localhost")).isEqualTo("ionic://localhost");
        assertThat(cors.checkOrigin("capacitor://localhost")).isEqualTo("capacitor://localhost");
        assertThat(cors.checkOrigin("http://localhost:4200")).isNull();
        assertThat(cors.checkOrigin("http://127.0.0.1:4200")).isNull();
        assertThat(cors.checkOrigin("https://evil.example.com")).isNull();
    }
}
