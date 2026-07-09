package com.irontrack.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validação real do JWT de acesso (07_ROADMAP_BACKEND.md §C.1 — o placeholder
 * da Sprint 0 é substituído aqui). Extrai {@code Authorization: Bearer <token>}
 * (03_CONTRATOS_API.md §1.3), valida via {@link JwtTokenProvider} e, se for um
 * token de acesso válido (nunca um refresh token), popula o
 * {@code SecurityContext} com o {@code userId} como principal — os
 * controllers autenticados leem esse valor via {@code Authentication#getName()}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtTokenProvider.validateToken(token) && !jwtTokenProvider.isRefreshToken(token)) {
                String userId = jwtTokenProvider.getUserId(token);
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
