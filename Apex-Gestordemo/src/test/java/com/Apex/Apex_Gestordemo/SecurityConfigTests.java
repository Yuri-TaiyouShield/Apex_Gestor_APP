package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTests {

    @Test
    void passwordEncoderUsesAdaptiveHash() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

        String encoded = encoder.encode("senha-forte-de-teste");

        assertThat(encoded).startsWith("$2");
        assertThat(encoder.matches("senha-forte-de-teste", encoded)).isTrue();
        assertThat(encoder.matches("senha-incorreta", encoded)).isFalse();
    }
}
