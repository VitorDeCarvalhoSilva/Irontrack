package com.irontrack.api.repositories;

import com.irontrack.api.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(String userId);

    /**
     * Revoga todos os refresh tokens ativos do usuário — usado por
     * reset de senha, troca de senha e solicitação de exclusão de conta
     * (03_CONTRATOS_API.md §2.7/§2.9/§2.10, mesma regra de segurança nos três).
     */
    default void revokeAllActiveTokensForUser(String userId) {
        Instant now = Instant.now();
        List<RefreshToken> active = findByUserIdAndRevokedAtIsNull(userId);
        active.forEach(refreshToken -> refreshToken.setRevokedAt(now));
        saveAll(active);
    }
}
