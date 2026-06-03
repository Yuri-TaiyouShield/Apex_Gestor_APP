package com.Apex.Apex_Gestordemo;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import Service.LicenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class LicenseBootstrapFilter extends OncePerRequestFilter {

    private static final String LICENSE_VALIDATE_PATH = "/api/licenses/validate";

    private final LicenseService licenseService;
    private final ObjectMapper objectMapper;

    public LicenseBootstrapFilter(LicenseService licenseService, ObjectMapper objectMapper) {
        this.licenseService = licenseService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LICENSE_VALIDATE_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        applyCorsHeaders(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        LicenseValidationRequestDTO body;
        try {
            body = objectMapper.readValue(request.getInputStream(), LicenseValidationRequestDTO.class);
        } catch (IOException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(response, "{\"valid\":false,\"status\":\"INVALID_JSON\",\"message\":\"JSON de licenca invalido.\"}");
            return;
        }

        LicenseValidationResponseDTO license = licenseService.validate(body);
        response.setStatus(HttpServletResponse.SC_OK);
        writeJson(response, toJson(license));
    }

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", isAllowedOrigin(origin) ? origin : "*");
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, X-Client-Platform");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    private boolean isAllowedOrigin(String origin) {
        return origin != null
                && (origin.startsWith("http://localhost:")
                || origin.startsWith("https://localhost:")
                || origin.endsWith(".trycloudflare.com")
                || origin.endsWith(".github.io")
                || "capacitor://localhost".equals(origin)
                || "ionic://localhost".equals(origin)
                || "app://localhost".equals(origin));
    }

    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json);
    }

    private String toJson(LicenseValidationResponseDTO license) {
        return "{"
                + "\"valid\":" + license.valid()
                + ",\"status\":\"" + escape(license.status()) + "\""
                + ",\"message\":\"" + escape(license.message()) + "\""
                + ",\"expiresAt\":" + nullable(license.expiresAt() == null ? null : license.expiresAt().toString())
                + ",\"remainingActivations\":" + (license.remainingActivations() == null ? "null" : license.remainingActivations())
                + ",\"deviceHash\":\"" + escape(license.deviceHash()) + "\""
                + ",\"appId\":\"" + escape(license.appId()) + "\""
                + ",\"licensePlan\":\"" + escape(license.licensePlan()) + "\""
                + ",\"allowedApps\":" + array(license.allowedApps())
                + ",\"activatedApps\":" + array(license.activatedApps())
                + "}";
    }

    private String nullable(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String array(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(",", values.stream()
                .map(value -> "\"" + escape(value) + "\"")
                .toList()) + "]";
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
