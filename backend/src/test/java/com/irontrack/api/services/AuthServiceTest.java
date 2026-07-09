package com.irontrack.api.services;

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
import com.irontrack.api.services.impl.AuthServiceImpl;
import com.irontrack.api.utils.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 3 — TDD escrito antes da implementação de
 * {@link AuthServiceImpl} (AGENTS.md §6.1).
 *
 * <p>{@code JwtTokenProvider} e {@code LoginAttemptService} são usados como
 * instâncias reais (não mocks): ambos já têm testes unitários dedicados
 * ({@code JwtTokenProviderTest}, {@code LoginAttemptServiceTest}) cobrindo
 * seu comportamento isoladamente, e mockar classes concretas via ByteBuddy
 * não funciona neste ambiente de execução (JDK 26) — usar as instâncias
 * reais também torna estes testes mais próximos do comportamento real.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String JWT_SECRET = "auth-service-test-secret-key-0123456789abcdef0123456789abcdef";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    private JwtTokenProvider jwtTokenProvider;
    private LoginAttemptService loginAttemptService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, 900_000L, 604_800_000L);
        loginAttemptService = new LoginAttemptService();
        authService = new AuthServiceImpl(userRepository, refreshTokenRepository, passwordEncoder,
                jwtTokenProvider, emailService, loginAttemptService);
    }

    // ---- register (§2.1) ----

    @Test
    void deveRegistrarNovoUsuarioJaComEmailVerificadoSemGerarTokenNemDispararEmail() {
        // Verificação de e-mail desativada no fluxo ativo (13_ADR_LOG.md ADR-018).
        RegisterRequest request = new RegisterRequest("Gabriel Silva", "gabriel.silva@email.com", "SenhaSegura123!");
        when(userRepository.existsByEmail("gabriel.silva@email.com")).thenReturn(false);
        when(passwordEncoder.encode("SenhaSegura123!")).thenReturn("hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        assertThat(response.name()).isEqualTo("Gabriel Silva");
        assertThat(response.email()).isEqualTo("gabriel.silva@email.com");
        assertThat(response.emailVerifiedAt()).isNotNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(saved.getEmailVerifiedAt()).isNotNull();
        assertThat(saved.getEmailVerificationTokenHash()).isNull();
        assertThat(saved.getEmailVerificationExpiresAt()).isNull();

        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void deveLancarBusinessRuleExceptionQuandoEmailJaCadastrado() {
        RegisterRequest request = new RegisterRequest("Gabriel Silva", "gabriel.silva@email.com", "SenhaSegura123!");
        when(userRepository.existsByEmail("gabriel.silva@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getErrorCode())
                        .isEqualTo("EMAIL_ALREADY_REGISTERED"));

        verify(userRepository, never()).save(any());
    }

    // ---- login (§2.2) ----

    @Test
    void deveLancarUnauthorizedQuandoCredenciaisInvalidas() {
        LoginRequest request = new LoginRequest("gabriel.silva@email.com", "SenhaErrada");
        when(userRepository.findByEmail("gabriel.silva@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveBloquearApos5TentativasDeLoginComCredenciaisInvalidas() {
        LoginRequest request = new LoginRequest("gabriel.silva@email.com", "SenhaErrada");
        when(userRepository.findByEmail("gabriel.silva@email.com")).thenReturn(Optional.empty());

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(request)).isInstanceOf(UnauthorizedException.class);
        }

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(ex -> assertThat(((TooManyRequestsException) ex).getErrorCode())
                        .isEqualTo("TOO_MANY_LOGIN_ATTEMPTS"));
    }

    @Test
    void devePermitirLoginMesmoComEmailVerifiedAtNuloPoisChecagemEstaDesativada() {
        // 13_ADR_LOG.md ADR-018: checagem de EMAIL_NOT_VERIFIED desativada no
        // fluxo ativo — login funciona mesmo se email_verified_at for null
        // (cenário legado/dormente, já que o registro atual sempre o preenche).
        LoginRequest request = new LoginRequest("gabriel.silva@email.com", "SenhaSegura123!");
        User user = umUsuarioValido();
        user.setEmailVerifiedAt(null);
        when(userRepository.findByEmail("gabriel.silva@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaSegura123!", user.getPasswordHash())).thenReturn(true);

        AuthTokensResponse response = authService.login(request);

        assertThat(jwtTokenProvider.validateToken(response.accessToken())).isTrue();
    }

    @Test
    void deveLancarForbiddenComAccountPendingDeletionQuandoExclusaoPendente() {
        LoginRequest request = new LoginRequest("gabriel.silva@email.com", "SenhaSegura123!");
        User user = umUsuarioValido();
        user.setDeletionRequestedAt(Instant.now());
        when(userRepository.findByEmail("gabriel.silva@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaSegura123!", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> assertThat(((ForbiddenException) ex).getErrorCode())
                        .isEqualTo("ACCOUNT_PENDING_DELETION"));
    }

    @Test
    void deveRetornarTokensValidosQuandoLoginBemSucedido() {
        LoginRequest request = new LoginRequest("gabriel.silva@email.com", "SenhaSegura123!");
        User user = umUsuarioValido();
        when(userRepository.findByEmail("gabriel.silva@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaSegura123!", user.getPasswordHash())).thenReturn(true);

        AuthTokensResponse response = authService.login(request);

        assertThat(jwtTokenProvider.validateToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.getUserId(response.accessToken())).isEqualTo(user.getId());
        assertThat(jwtTokenProvider.isRefreshToken(response.accessToken())).isFalse();
        assertThat(jwtTokenProvider.isRefreshToken(response.refreshToken())).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void deveResetarContadorDeFalhasAposLoginBemSucedido() {
        User user = umUsuarioValido();
        LoginRequest badRequest = new LoginRequest(user.getEmail(), "SenhaErrada");
        LoginRequest goodRequest = new LoginRequest(user.getEmail(), "SenhaSegura123!");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", user.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.matches("SenhaSegura123!", user.getPasswordHash())).thenReturn(true);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(badRequest)).isInstanceOf(UnauthorizedException.class);
        }
        authService.login(goodRequest);

        // Se o reset não tivesse ocorrido, esta 5a falha "nova" já bloquearia (429).
        assertThatThrownBy(() -> authService.login(badRequest)).isInstanceOf(UnauthorizedException.class);
    }

    // ---- refresh (§2.4) ----

    @Test
    void deveLancarUnauthorizedQuandoJwtDeRefreshInvalido() {
        RefreshRequest request = new RefreshRequest("isto-nao-e-um-jwt");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void deveLancarUnauthorizedQuandoUmAccessTokenEEnviadoParaRefresh() {
        String accessToken = jwtTokenProvider.generateAccessToken("usr-1");

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(accessToken)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveLancarUnauthorizedQuandoRefreshTokenNaoEncontradoNoBanco() {
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken("usr-1");
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(rawRefreshToken)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveLancarUnauthorizedQuandoRefreshTokenJaRevogado() {
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken("usr-1");
        RefreshToken stored = new RefreshToken();
        stored.setUserId("usr-1");
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        stored.setRevokedAt(Instant.now());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(rawRefreshToken)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deveRotacionarTokenRevogandoOAntigoEEmitindoNovoParQuandoValido() {
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken("usr-1");
        RefreshToken stored = new RefreshToken();
        stored.setUserId("usr-1");
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawRefreshToken)))
                .thenReturn(Optional.of(stored));

        AuthTokensResponse response = authService.refresh(new RefreshRequest(rawRefreshToken));

        assertThat(jwtTokenProvider.validateToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.getUserId(response.accessToken())).isEqualTo("usr-1");
        assertThat(jwtTokenProvider.isRefreshToken(response.refreshToken())).isTrue();
        assertThat(response.refreshToken()).isNotEqualTo(rawRefreshToken);
        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    // ---- logout (§2.5) ----

    @Test
    void deveRevogarRefreshTokenExistenteNoLogout() {
        RefreshRequest request = new RefreshRequest("token-existente");
        RefreshToken stored = new RefreshToken();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout(request);

        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logoutDeveSerIdempotenteQuandoTokenNaoExiste() {
        RefreshRequest request = new RefreshRequest("token-inexistente");
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout(request);

        verify(refreshTokenRepository, never()).save(any());
    }

    // ---- verifyEmail (§2.6) ----

    @Test
    void deveVerificarEmailComTokenValidoELimparCampos() {
        User user = umUsuarioValido();
        user.setEmailVerifiedAt(null);
        user.setEmailVerificationTokenHash("hash-qualquer");
        user.setEmailVerificationExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByEmailVerificationTokenHash(anyString())).thenReturn(Optional.of(user));

        VerifyEmailResponse response = authService.verifyEmail("raw-token");

        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getEmailVerificationTokenHash()).isNull();
        assertThat(user.getEmailVerificationExpiresAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void deveLancarBadRequestQuandoTokenDeVerificacaoNaoEncontrado() {
        when(userRepository.findByEmailVerificationTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("token-invalido"))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_OR_EXPIRED_TOKEN"));
    }

    @Test
    void deveLancarBadRequestQuandoTokenDeVerificacaoExpirado() {
        User user = umUsuarioValido();
        user.setEmailVerificationExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findByEmailVerificationTokenHash(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail("token-expirado")).isInstanceOf(BadRequestException.class);
    }

    // ---- forgotPassword / resetPassword (§2.7) ----

    @Test
    void forgotPasswordDeveEnviarEmailQuandoUsuarioExisteMasResponderGenerico() {
        User user = umUsuarioValido();
        when(userRepository.findByEmail("gabriel.silva@email.com")).thenReturn(Optional.of(user));

        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest("gabriel.silva@email.com"));

        assertThat(response.message()).isNotBlank();
        verify(emailService).sendPasswordResetEmail(eq("gabriel.silva@email.com"), anyString());
        verify(userRepository).save(user);
        assertThat(user.getPasswordResetTokenHash()).isNotBlank();
        assertThat(user.getPasswordResetExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void forgotPasswordNaoDeveEnviarEmailNemRevelarQuandoUsuarioNaoExiste() {
        when(userRepository.findByEmail("desconhecido@email.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest("desconhecido@email.com"));

        assertThat(response.message()).isNotBlank();
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void resetPasswordDeveAtualizarSenhaERevogarTodosOsRefreshTokens() {
        User user = umUsuarioValido();
        user.setPasswordResetTokenHash("hash-reset");
        user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        when(userRepository.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NovaSenhaSegura456!")).thenReturn("novo-hash-bcrypt");

        authService.resetPassword(new ResetPasswordRequest("raw-reset-token", "NovaSenhaSegura456!"));

        assertThat(user.getPasswordHash()).isEqualTo("novo-hash-bcrypt");
        assertThat(user.getPasswordResetTokenHash()).isNull();
        assertThat(user.getPasswordResetExpiresAt()).isNull();
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllActiveTokensForUser(user.getId());
    }

    @Test
    void resetPasswordDeveLancarBadRequestQuandoTokenInvalidoOuExpirado() {
        when(userRepository.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("token-invalido", "NovaSenha123!")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_OR_EXPIRED_RESET_TOKEN"));
    }

    private User umUsuarioValido() {
        User user = new User();
        user.setId("usr-1");
        user.setName("Gabriel Silva");
        user.setEmail("gabriel.silva@email.com");
        user.setPasswordHash("hash-bcrypt-existente");
        user.setEmailVerifiedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        user.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        user.setUpdatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        return user;
    }
}
