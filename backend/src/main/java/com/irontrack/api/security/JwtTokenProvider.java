package com.irontrack.api.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Geração/validação de tokens JWT HMAC (07_ROADMAP_BACKEND.md §C.1, item 6),
 * usando `io.jsonwebtoken:jjwt` (01_ARQUITETURA_E_PADROES.md §2.4). Access e
 * refresh tokens são ambos JWTs assinados com a mesma chave simétrica,
 * diferenciados pela claim {@code type} — o refresh token, além de válido
 * como JWT, também precisa ter seu hash presente e não revogado em
 * {@code refresh_tokens} (verificado pelo `AuthService`, não aqui).
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${irontrack.jwt.secret-key}") String secretKey,
            @Value("${irontrack.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${irontrack.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(String userId) {
        return buildToken(userId, TYPE_ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, TYPE_REFRESH, refreshTokenExpirationMs);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private String buildToken(String userId, String type, long expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    private io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
