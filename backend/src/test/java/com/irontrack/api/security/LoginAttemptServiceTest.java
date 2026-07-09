package com.irontrack.api.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 05_DEVOPS_E_SEGURANCA.md §E.2: bloqueio de 15 minutos após 5 falhas
 * consecutivas de login para o mesmo e-mail (contador Caffeine em memória).
 * Peça de regra de negócio mais pura desta sprint — testada isoladamente,
 * antes de qualquer integração com {@code AuthService} (AGENTS.md §6.1).
 */
class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void naoDeveBloquearQuandoNaoHaTentativasFalhas() {
        assertThat(service.isBlocked("gabriel.silva@email.com")).isFalse();
    }

    @Test
    void naoDeveBloquearComMenosDeCincoFalhas() {
        for (int i = 0; i < 4; i++) {
            service.registerFailure("gabriel.silva@email.com");
        }

        assertThat(service.isBlocked("gabriel.silva@email.com")).isFalse();
    }

    @Test
    void deveBloquearAposCincoFalhasConsecutivas() {
        for (int i = 0; i < 5; i++) {
            service.registerFailure("gabriel.silva@email.com");
        }

        assertThat(service.isBlocked("gabriel.silva@email.com")).isTrue();
    }

    @Test
    void naoDeveAfetarContadorDeOutroEmail() {
        for (int i = 0; i < 5; i++) {
            service.registerFailure("gabriel.silva@email.com");
        }

        assertThat(service.isBlocked("outro.usuario@email.com")).isFalse();
    }

    @Test
    void deveDesbloquearAposReset() {
        for (int i = 0; i < 5; i++) {
            service.registerFailure("gabriel.silva@email.com");
        }

        service.reset("gabriel.silva@email.com");

        assertThat(service.isBlocked("gabriel.silva@email.com")).isFalse();
    }

    /**
     * Achado da auditoria de hardening (prompt-backend-hardening.md §1.1):
     * o contador precisa ser case-insensitive para {@code email} — "Foo@X.com"
     * e "foo@x.com" são o mesmo endereço na prática e devem compartilhar o
     * mesmo contador de falhas, senão um atacante contorna o rate limit
     * trivialmente variando a capitalização a cada tentativa.
     */
    @Test
    void contadorDeveSerCaseInsensitiveParaOMesmoEmail() {
        for (int i = 0; i < 4; i++) {
            service.registerFailure("Foo@X.com");
        }
        service.registerFailure("foo@x.com");

        assertThat(service.isBlocked("FOO@X.COM")).isTrue();
        assertThat(service.isBlocked("foo@x.com")).isTrue();
    }

    @Test
    void resetDeveSerCaseInsensitiveParaOMesmoEmail() {
        for (int i = 0; i < 5; i++) {
            service.registerFailure("Foo@X.com");
        }

        service.reset("FOO@X.COM");

        assertThat(service.isBlocked("foo@x.com")).isFalse();
    }
}
