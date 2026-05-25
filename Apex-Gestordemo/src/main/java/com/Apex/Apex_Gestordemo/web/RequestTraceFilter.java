package com.Apex.Apex_Gestordemo.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_ATTRIBUTE = "apexTraceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        long startedAt = System.currentTimeMillis();

        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        MDC.put("traceId", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            LOGGER.info("http_request method={} path={} status={} durationMs={} remote={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsedMs,
                    request.getRemoteAddr());
            MDC.remove("traceId");
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String providedTraceId = request.getHeader(TRACE_ID_HEADER);
        if (providedTraceId == null || providedTraceId.isBlank() || providedTraceId.length() > 80) {
            return UUID.randomUUID().toString();
        }
        return providedTraceId;
    }
}
