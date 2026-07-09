package com.irontrack.api.services.impl;

import com.irontrack.api.dto.request.ForgotPasswordRequest;
import com.irontrack.api.dto.request.LoginRequest;
import com.irontrack.api.dto.request.RefreshRequest;
import com.irontrack.api.dto.request.RegisterRequest;
import com.irontrack.api.dto.request.ResetPasswordRequest;
import com.irontrack.api.dto.response.AuthTokensResponse;
import com.irontrack.api.dto.response.MessageResponse;
import com.irontrack.api.dto.response.UserResponse;
import com.irontrack.api.dto.response.VerifyEmailResponse;
import com.irontrack.api.entities.RefreshToken;
import com.irontrack.api.entities.User;
import com.irontrack.api.exceptions.BadRequestException;
import com.irontrack.api.exceptions.BusinessRuleException;
import com.irontrack.api.exceptions.ForbiddenException;
import com.irontrack.api.exceptions.TooManyRequestsException;
import com.irontrack.api.exceptions.UnauthorizedException;
import com.irontrack.api.repositories.RefreshTokenRepository;
import com.irontrack.api.repositories.UserRepository;
import com.irontrack.api.security.JwtTokenProvider;
import com.irontrack.api.security.LoginAttemptService;
import com.irontrack.api.services.AuthService;
import com.irontrack.api.services.EmailService;
import com.irontrack.api.utils.TokenHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 3. Testado em {@code AuthServiceTest}
 * (TDD — testes escritos antes desta implementação, AGENTS.md §6.1).
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 03_CONTRATOS_API.md §2.7 / 00_PRD_IRONTRACK.md §4.3: validade de 1 hora. */
    private static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);

    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "Se o e-mail informado estiver cadastrado, você receberá instruções para redefinir sua senha.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    public AuthServiceImpl(UserRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            PasswordEncoder passwordEncoder,
                            JwtTokenProvider jwtTokenProvider,
                            EmailService emailService,
                            LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Este e-mail já está cadastrado.", "EMAIL_ALREADY_REGISTERED");
        }

        Instant now = Instant.now();

        // Verificação de e-mail desativada no fluxo ativo (13_ADR_LOG.md ADR-018):
        // conta já nasce verificada, nenhum token é gerado, nenhum e-mail é disparado.
        // email_verification_token_hash/email_verification_expires_at ficam null
        // (colunas dormentes, reativação futura sem nova migração).
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerifiedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User saved = userRepository.save(user);

        return toUserResponse(saved);
    }

    @Override
    @Transactional
    public AuthTokensResponse login(LoginRequest request) {
        if (loginAttemptService.isBlocked(request.email())) {
            throw new TooManyRequestsException(
                    "Muitas tentativas de login. Tente novamente mais tarde.", "TOO_MANY_LOGIN_ATTEMPTS");
        }

        Optional<User> maybeUser = userRepository.findByEmail(request.email());
        if (maybeUser.isEmpty() || !passwordEncoder.matches(request.password(), maybeUser.get().getPasswordHash())) {
            loginAttemptService.registerFailure(request.email());
            throw new UnauthorizedException("E-mail ou senha inválidos.");
        }

        User user = maybeUser.get();

        if (user.getDeletionRequestedAt() != null) {
            throw new ForbiddenException("Conta em processo de exclusão.", "ACCOUNT_PENDING_DELETION");
        }
        // Checagem de email_verified_at desativada (13_ADR_LOG.md ADR-018) - todo
        // registro já nasce verificado, então a condição nunca mais seria verdadeira.
        // errorCode EMAIL_NOT_VERIFIED fica dormente no catálogo, não removido.

        loginAttemptService.reset(request.email());

        return issueTokenPair(user.getId());
    }

    @Override
    @Transactional
    public AuthTokensResponse refresh(RefreshRequest request) {
        String rawRefreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(rawRefreshToken) || !jwtTokenProvider.isRefreshToken(rawRefreshToken)) {
            throw new UnauthorizedException("Refresh token inválido.", "INVALID_REFRESH_TOKEN");
        }

        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawRefreshToken))
                .filter(refreshToken -> refreshToken.getRevokedAt() == null)
                .filter(refreshToken -> refreshToken.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new UnauthorizedException(
                        "Refresh token inválido, expirado ou revogado.", "INVALID_REFRESH_TOKEN"));

        // Rotação obrigatória (03_CONTRATOS_API.md §2.4): nunca reemite o mesmo refresh token.
        stored.setRevokedAt(now);
        refreshTokenRepository.save(stored);

        return issueTokenPair(stored.getUserId());
    }

    @Override
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(TokenHasher.hash(request.refreshToken()))
                .filter(refreshToken -> refreshToken.getRevokedAt() == null)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Override
    @Transactional
    public VerifyEmailResponse verifyEmail(String rawToken) {
        Instant now = Instant.now();
        User user = userRepository.findByEmailVerificationTokenHash(TokenHasher.hash(rawToken))
                .filter(candidate -> candidate.getEmailVerificationExpiresAt() != null
                        && candidate.getEmailVerificationExpiresAt().isAfter(now))
                .orElseThrow(() -> new BadRequestException(
                        "Token de verificação inválido ou expirado.", "INVALID_OR_EXPIRED_TOKEN"));

        user.setEmailVerifiedAt(now);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        user.setUpdatedAt(now);
        userRepository.save(user);

        return new VerifyEmailResponse(user.getEmail(), now);
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            Instant now = Instant.now();
            String rawResetToken = TokenHasher.generateRawToken();
            user.setPasswordResetTokenHash(TokenHasher.hash(rawResetToken));
            user.setPasswordResetExpiresAt(now.plus(PASSWORD_RESET_TTL));
            user.setUpdatedAt(now);
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), rawResetToken);
        });

        // Nunca revela se o e-mail existe (03_CONTRATOS_API.md §2.7).
        return new MessageResponse(GENERIC_FORGOT_PASSWORD_MESSAGE);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();
        User user = userRepository.findByPasswordResetTokenHash(TokenHasher.hash(request.token()))
                .filter(candidate -> candidate.getPasswordResetExpiresAt() != null
                        && candidate.getPasswordResetExpiresAt().isAfter(now))
                .orElseThrow(() -> new BadRequestException(
                        "Token de redefinição inválido ou expirado.", "INVALID_OR_EXPIRED_RESET_TOKEN"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        user.setUpdatedAt(now);
        userRepository.save(user);

        // Força logout de todas as sessões/dispositivos (03_CONTRATOS_API.md §2.7).
        refreshTokenRepository.revokeAllActiveTokensForUser(user.getId());
    }

    private AuthTokensResponse issueTokenPair(String userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(userId);

        Instant now = Instant.now();
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserId(userId);
        refreshTokenEntity.setTokenHash(TokenHasher.hash(refreshTokenValue));
        refreshTokenEntity.setExpiresAt(now.plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()));
        refreshTokenEntity.setCreatedAt(now);
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthTokensResponse(accessToken, refreshTokenValue);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getEmailVerifiedAt(), user.getCreatedAt());
    }
}
