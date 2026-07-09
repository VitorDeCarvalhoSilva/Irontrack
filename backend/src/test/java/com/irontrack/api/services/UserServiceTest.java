package com.irontrack.api.services;

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
import com.irontrack.api.services.PushSubscriptionCleanupService;
import com.irontrack.api.repositories.RefreshTokenRepository;
import com.irontrack.api.repositories.UserRepository;
import com.irontrack.api.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 4 — TDD escrito antes da implementação de
 * {@link UserServiceImpl} (AGENTS.md §6.1).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private PushSubscriptionCleanupService pushSubscriptionCleanupService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, refreshTokenRepository, passwordEncoder,
                emailService, pushSubscriptionCleanupService);
    }

    // ---- me (§2.3) ----

    @Test
    void meDeveRetornarDadosDoUsuarioAutenticado() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));

        UserResponse response = userService.me("usr-1");

        assertThat(response.id()).isEqualTo("usr-1");
        assertThat(response.email()).isEqualTo("gabriel.silva@email.com");
    }

    @Test
    void meDeveLancarResourceNotFoundQuandoUsuarioNaoExiste() {
        when(userRepository.findById("usr-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.me("usr-inexistente")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- updateProfile (§2.8) ----

    @Test
    void updateProfileDeveAtualizarApenasNomeQuandoEmailNaoInformado() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile("usr-1", new UpdateProfileRequest("Gabriel S. Santos", null));

        assertThat(response.name()).isEqualTo("Gabriel S. Santos");
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void updateProfileNaoDeveExigirNovaVerificacaoQuandoEmailEnviadoIgualAoAtual() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateProfile("usr-1", new UpdateProfileRequest(null, user.getEmail()));

        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void updateProfileDeveExigirNovaVerificacaoQuandoEmailMuda() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile("usr-1",
                new UpdateProfileRequest(null, "gabriel.novo@email.com"));

        assertThat(response.email()).isEqualTo("gabriel.novo@email.com");
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getEmailVerificationTokenHash()).isNotBlank();
        assertThat(user.getEmailVerificationExpiresAt()).isAfter(Instant.now());
        verify(emailService).sendVerificationEmail(eq("gabriel.novo@email.com"), anyString(), anyString());
    }

    // ---- changePassword (§2.9) ----

    @Test
    void changePasswordDeveAtualizarSenhaERevogarTodosOsRefreshTokensQuandoSenhaAtualCorreta() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaAtual123!", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("NovaSenhaSegura456!")).thenReturn("novo-hash-bcrypt");

        userService.changePassword("usr-1", new ChangePasswordRequest("SenhaAtual123!", "NovaSenhaSegura456!"));

        assertThat(user.getPasswordHash()).isEqualTo("novo-hash-bcrypt");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllActiveTokensForUser("usr-1");
    }

    @Test
    void changePasswordDeveLancarUnauthorizedComInvalidCurrentPasswordQuandoSenhaAtualIncorreta() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("usr-1",
                new ChangePasswordRequest("SenhaErrada", "NovaSenhaSegura456!")))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                        .isEqualTo("INVALID_CURRENT_PASSWORD"));

        verify(refreshTokenRepository, never()).revokeAllActiveTokensForUser(any());
    }

    // ---- requestDeletion (§2.10) ----

    @Test
    void requestDeletionDeveMarcarExclusaoRevogarTokensERemoverPushSubscriptionsQuandoSenhaCorreta() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaAtual123!", user.getPasswordHash())).thenReturn(true);

        DeletionScheduledResponse response = userService.requestDeletion("usr-1",
                new DeleteAccountRequest("SenhaAtual123!"));

        assertThat(user.getDeletionRequestedAt()).isNotNull();
        assertThat(response.deletionScheduledFor()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllActiveTokensForUser("usr-1");
        verify(pushSubscriptionCleanupService).deleteAllForUser("usr-1");
    }

    @Test
    void requestDeletionDeveLancarUnauthorizedComInvalidPasswordQuandoSenhaIncorreta() {
        User user = umUsuarioValido();
        when(userRepository.findById("usr-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.requestDeletion("usr-1", new DeleteAccountRequest("SenhaErrada")))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                        .isEqualTo("INVALID_PASSWORD"));

        assertThat(user.getDeletionRequestedAt()).isNull();
    }

    // ---- cancelDeletion (§2.11) ----

    @Test
    void cancelDeletionDeveLimparDeletionRequestedAtQuandoCredenciaisCorretasEExclusaoPendente() {
        User user = umUsuarioValido();
        user.setDeletionRequestedAt(Instant.now().minus(5, ChronoUnit.DAYS));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaAtual123!", user.getPasswordHash())).thenReturn(true);

        userService.cancelDeletion(new CancelDeletionRequest(user.getEmail(), "SenhaAtual123!"));

        assertThat(user.getDeletionRequestedAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void cancelDeletionDeveLancarUnauthorizedQuandoUsuarioNaoExiste() {
        when(userRepository.findByEmail("desconhecido@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.cancelDeletion(
                new CancelDeletionRequest("desconhecido@email.com", "QualquerSenha123!")))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                        .isEqualTo("INVALID_PASSWORD"));
    }

    @Test
    void cancelDeletionDeveLancarUnauthorizedQuandoSenhaIncorreta() {
        User user = umUsuarioValido();
        user.setDeletionRequestedAt(Instant.now());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.cancelDeletion(new CancelDeletionRequest(user.getEmail(), "SenhaErrada")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void cancelDeletionDeveLancarBadRequestComNoPendingDeletionQuandoNaoHaExclusaoPendente() {
        User user = umUsuarioValido();
        user.setDeletionRequestedAt(null);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaAtual123!", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> userService.cancelDeletion(new CancelDeletionRequest(user.getEmail(), "SenhaAtual123!")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("NO_PENDING_DELETION"));
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
