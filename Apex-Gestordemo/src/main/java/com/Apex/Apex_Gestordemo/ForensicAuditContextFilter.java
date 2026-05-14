package com.Apex.Apex_Gestordemo;

import Model.AuditRevisionContext;
import Model.AuditRevisionContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(90)
public class ForensicAuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            AuditRevisionContextHolder.set(new AuditRevisionContext(
                    actor(),
                    sourceIp(request),
                    request.getHeader("User-Agent"),
                    correlationId(request)
            ));
            filterChain.doFilter(request, response);
        } finally {
            AuditRevisionContextHolder.clear();
        }
    }

    private String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private String sourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String correlationId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
