package com.irontrack.api.services.impl;

import com.irontrack.api.dto.request.CancelDeletionRequest;
import com.irontrack.api.dto.request.ChangePasswordRequest;
import com.irontrack.api.dto.request.DeleteAccountRequest;
import com.irontrack.api.dto.request.UpdateProfileRequest;
import com.irontrack.api.dto.response.DeletionScheduledResponse;
import com.irontrack.api.dto.response.UserResponse;
import com.irontrack.api.entities.User;
import com.irontrack.api.exceptions.BadRequestException;
import com.irontrack.api.exceptions.ResourceNotFoundException;
import com.irontrack.api.exceptions.UnauthorizedException;
import com.irontrack.api.repositories.RefreshTokenRepository;
import com.irontrack.api.repositories.UserRepository;
import com.irontrack.api.services.EmailService;
import com.irontrack.api.services.PushSubscriptionCleanupService;
import com.irontrack.api.services.UserService;
import com.irontrack.api.utils.TokenHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 4. Testado em {@code UserServiceTest}
 * (TDD — testes escritos antes desta implementação, AGENTS.md §6.1).
 */
@Service
public class UserServiceImpl implements UserService {

    /** Mesmo TTL de verificação usado no cadastro (AuthServiceImpl). */
    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);

    /** 11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md §B.2: carência de 30 dias. */
    private static final Duration DELETION_GRACE_PERIOD = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PushSubscriptionCleanupService pushSubscriptionCleanupService;

    public UserServiceImpl(UserRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            PasswordEncoder passwordEncoder,
                            EmailService emailService,
                            PushSubscriptionCleanupService pushSubscriptionCleanupService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.pushSubscriptionCleanupService = pushSubscriptionCleanupService;
    }

    @Override
    public UserResponse me(String userId) {
        return toUserResponse(findUserOrThrow(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);
        Instant now = Instant.now();

        if (request.name() != null) {
            user.setName(request.name());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            String rawVerificationToken = TokenHasher.generateRawToken();
            user.setEmail(request.email());
            user.setEmailVerifiedAt(null);
            user.setEmailVerificationTokenHash(TokenHasher.hash(rawVerificationToken));
            user.setEmailVerificationExpiresAt(now.plus(EMAIL_VERIFICATION_TTL));
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), rawVerificationToken);
        }

        user.setUpdatedAt(now);
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Senha atual incorreta.", "INVALID_CURRENT_PASSWORD");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllActiveTokensForUser(userId);
    }

    @Override
    @Transactional
    public DeletionScheduledResponse requestDeletion(String userId, DeleteAccountRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Senha incorreta.", "INVALID_PASSWORD");
        }

        Instant now = Instant.now();
        user.setDeletionRequestedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        refreshTokenRepository.revokeAllActiveTokensForUser(userId);
        pushSubscriptionCleanupService.deleteAllForUser(userId);

        return new DeletionScheduledResponse(now.plus(DELETION_GRACE_PERIOD));
    }

    @Override
    @Transactional
    public void cancelDeletion(CancelDeletionRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha inválidos.", "INVALID_PASSWORD"));

        if (user.getDeletionRequestedAt() == null) {
            throw new BadRequestException("Não há exclusão pendente para esta conta.", "NO_PENDING_DELETION");
        }

        user.setDeletionRequestedAt(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getEmailVerifiedAt(), user.getCreatedAt());
    }
}
