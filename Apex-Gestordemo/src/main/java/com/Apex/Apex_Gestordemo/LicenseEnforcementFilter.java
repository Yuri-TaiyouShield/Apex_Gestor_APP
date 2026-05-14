package com.Apex.Apex_Gestordemo;

import DTO.LicenseValidationResponseDTO;
import Service.LicenseService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class LicenseEnforcementFilter extends OncePerRequestFilter {

    public static final String LICENSE_KEY_HEADER = "X-Apex-License-Key";
    public static final String DEVICE_FINGERPRINT_HEADER = "X-Apex-Device-Fingerprint";
    public static final String APP_ID_HEADER = "X-Apex-App-Id";

    private static final Set<String> PUBLIC_API_PREFIXES = Set.of(
            "/api/auth/",
            "/api/licenses/",
            "/api/privacy/"
    );

    private final LicenseService licenseService;

    public LicenseEnforcementFilter(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !uri.startsWith("/api/")
                || PUBLIC_API_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        LicenseValidationResponseDTO license = licenseService.checkActivation(
                request.getHeader(LICENSE_KEY_HEADER),
                request.getHeader(DEVICE_FINGERPRINT_HEADER),
                request.getHeader(APP_ID_HEADER),
                request.getHeader("X-Client-Platform")
        );

        if (!license.valid()) {
            response.setStatus(402);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(toJson(license));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String toJson(LicenseValidationResponseDTO license) {
        return "{"
                + "\"valid\":" + license.valid()
                + ",\"status\":\"" + escape(license.status()) + "\""
                + ",\"message\":\"" + escape(license.message()) + "\""
                + ",\"appId\":\"" + escape(license.appId()) + "\""
                + ",\"licensePlan\":\"" + escape(license.licensePlan()) + "\""
                + ",\"allowedApps\":" + array(license.allowedApps())
                + ",\"activatedApps\":" + array(license.activatedApps())
                + "}";
    }

    private String array(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .map(value -> "\"" + escape(value) + "\"")
                .toList()
                .toString()
                .replace(" ", "");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
