package com.irontrack.api.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * 07_ROADMAP_BACKEND.md §C.1: a validação real do token (placeholder desde a
 * Sprint 0) passa a popular o {@code SecurityContext} a partir do JWT de
 * acesso enviado em {@code Authorization: Bearer <token>}
 * (03_CONTRATOS_API.md §1.3).
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "jwt-filter-test-secret-key-0123456789abcdef0123456789abcdef";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

    @BeforeEach
    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarQuandoTokenDeAcessoValido() throws Exception {
        String accessToken = jwtTokenProvider.generateAccessToken("usr-1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("usr-1");
        verify(chain).doFilter(request, response);
    }

    @Test
    void naoDeveAutenticarQuandoHeaderAusente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void naoDeveAutenticarQuandoTokenInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer isto-nao-e-um-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void naoDeveAutenticarQuandoTokenERefreshTokenEmVezDeAcesso() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken("usr-1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
