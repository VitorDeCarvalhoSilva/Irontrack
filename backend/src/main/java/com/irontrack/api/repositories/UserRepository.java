package com.irontrack.api.repositories;

import com.irontrack.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationTokenHash(String emailVerificationTokenHash);

    Optional<User> findByPasswordResetTokenHash(String passwordResetTokenHash);

    /**
     * 11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md §B.4: contas cujo
     * período de carência de 30 dias já expirou, candidatas à exclusão física
     * pelo {@code AccountDeletionSchedulerService}.
     */
    List<User> findByDeletionRequestedAtBefore(Instant threshold);
}
