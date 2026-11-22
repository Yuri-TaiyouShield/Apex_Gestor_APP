package com.Apex.Apex_Gestordemo.security;

import Service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                var claims = jwtTokenService.parseAndValidate(authorization.substring(7).trim());
                var authorities = claims.roles().stream().map(SimpleGrantedAuthority::new).toList();
                var authentication = new UsernamePasswordAuthenticationToken(claims.login(), null, authorities);
                authentication.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalido ou expirado.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
