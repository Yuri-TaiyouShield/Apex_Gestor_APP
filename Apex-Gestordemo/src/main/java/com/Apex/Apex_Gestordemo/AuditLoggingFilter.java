package com.Apex.Apex_Gestordemo;

import Service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditLogService auditLogService;

    public AuditLoggingFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/") && MUTATING_METHODS.contains(request.getMethod())) {
                try {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    auditLogService.record(request, response.getStatus(), authentication);
                } catch (RuntimeException ignored) {
                    // Auditoria nao pode derrubar a operacao principal.
                }
            }
        }
    }
}
