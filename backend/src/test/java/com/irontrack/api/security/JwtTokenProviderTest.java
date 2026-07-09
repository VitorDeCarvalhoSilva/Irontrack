package com.irontrack.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 6: geração/validação de tokens HMAC via
 * jjwt, com os tempos de expiração de 05_DEVOPS_E_SEGURANCA.md §D.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-key-0123456789abcdef0123456789abcdef-not-real";

    @Test
    void deveGerarTokenDeAcessoContendoOSubjectCorreto() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);

        String token = provider.generateAccessToken("usr-123");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo("usr-123");
        assertThat(provider.isRefreshToken(token)).isFalse();
    }

    @Test
    void deveGerarRefreshTokenMarcadoComoTipoRefresh() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);

        String token = provider.generateRefreshToken("usr-456");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo("usr-456");
        assertThat(provider.isRefreshToken(token)).isTrue();
    }

    @Test
    void deveRejeitarTokenComAssinaturaDeOutraChave() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);
        JwtTokenProvider outroEmissor = new JwtTokenProvider(
                "outra-chave-completamente-diferente-0123456789abcdef0123456789", 900_000L, 604_800_000L);

        String tokenDeOutroEmissor = outroEmissor.generateAccessToken("usr-789");

        assertThat(provider.validateToken(tokenDeOutroEmissor)).isFalse();
    }

    @Test
    void deveRejeitarTokenExpirado() throws InterruptedException {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 1L, 1L);

        String token = provider.generateAccessToken("usr-expira");
        Thread.sleep(20);

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void deveRejeitarTokenMalformado() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);

        assertThat(provider.validateToken("isto-nao-e-um-jwt")).isFalse();
    }
}
