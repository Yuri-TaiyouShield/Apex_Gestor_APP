package com.Apex.Apex_Gestordemo;

import DTO.LicenseValidationResponseDTO;
import Service.LicenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(name = "apex.license.enforcement.enabled", havingValue = "true", matchIfMissing = true)
public class LicenseEnforcementFilter extends OncePerRequestFilter {

    public static final String LICENSE_KEY_HEADER = "X-Apex-License-Key";
    public static final String DEVICE_FINGERPRINT_HEADER = "X-Apex-Device-Fingerprint";
    public static final String APP_ID_HEADER = "X-Apex-App-Id";

    private static final Set<String> PUBLIC_API_PREFIXES = Set.of(
            "/api/auth/",
            "/api/licenses/",
            "/api/privacy/consents"
    );

    private final LicenseService licenseService;
    private final ObjectMapper objectMapper;

    @Autowired
    public LicenseEnforcementFilter(LicenseService licenseService) {
        this(licenseService, new ObjectMapper().findAndRegisterModules());
    }

    public LicenseEnforcementFilter(LicenseService licenseService, ObjectMapper objectMapper) {
        this.licenseService = licenseService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !uri.startsWith("/api/")
                || PUBLIC_API_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        LicenseValidationResponseDTO license = licenseService.checkActivation(
                request.getHeader(LICENSE_KEY_HEADER),
                request.getHeader(DEVICE_FINGERPRINT_HEADER),
                request.getHeader(APP_ID_HEADER),
                request.getHeader("X-Client-Platform")
        );

        if (!license.valid()) {
            response.setStatus(402);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(license));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
