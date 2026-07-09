package com.irontrack.api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Contador de falhas de login em memória (05_DEVOPS_E_SEGURANCA.md §E.2):
 * bloqueia por 15 minutos após 5 falhas consecutivas para o mesmo e-mail.
 * Caffeine, sem dependência externa (Redis) — limitação assumida
 * deliberadamente: um reinício do processo zera os contadores.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_WINDOW = Duration.ofMinutes(15);

    private final Cache<String, Integer> attemptsByEmail = Caffeine.newBuilder()
            .expireAfterWrite(BLOCK_WINDOW)
            .build();

    public void registerFailure(String email) {
        attemptsByEmail.asMap().merge(normalize(email), 1, Integer::sum);
    }

    public boolean isBlocked(String email) {
        Integer attempts = attemptsByEmail.getIfPresent(normalize(email));
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }

    public void reset(String email) {
        attemptsByEmail.invalidate(normalize(email));
    }

    /**
     * E-mails são case-insensitive na prática — sem isso, um atacante
     * contornaria o rate limit trivialmente variando a capitalização a cada
     * tentativa (achado da auditoria de hardening, prompt-backend-hardening.md §1.1).
     */
    private static String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
