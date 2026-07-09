package com.irontrack.api.services;

import com.irontrack.api.entities.User;
import com.irontrack.api.repositories.UserRepository;
import com.irontrack.api.services.impl.AccountDeletionSchedulerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 5 /
 * 11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md §B.4 — TDD escrito antes
 * da implementação (AGENTS.md §6.1).
 */
@ExtendWith(MockitoExtension.class)
class AccountDeletionSchedulerServiceTest {

    @Mock
    private UserRepository userRepository;

    private AccountDeletionSchedulerService service;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionSchedulerServiceImpl(userRepository);
    }

    @Test
    void deveExcluirFisicamenteContasCujoPeriodoDeCarenciaJaExpirou() {
        User expirado1 = umUsuarioComDeletionRequestedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        User expirado2 = umUsuarioComDeletionRequestedAt(Instant.now().minus(45, ChronoUnit.DAYS));
        when(userRepository.findByDeletionRequestedAtBefore(any())).thenReturn(List.of(expirado1, expirado2));

        service.deleteExpiredAccounts();

        verify(userRepository).delete(expirado1);
        verify(userRepository).delete(expirado2);
        verify(userRepository, times(2)).delete(any());
    }

    @Test
    void naoDeveExcluirNadaQuandoNaoHaContasElegiveis() {
        when(userRepository.findByDeletionRequestedAtBefore(any())).thenReturn(List.of());

        service.deleteExpiredAccounts();

        verify(userRepository, never()).delete(any());
    }

    @Test
    void deveConsultarComThresholdDeAproximadamente30DiasAtras() {
        when(userRepository.findByDeletionRequestedAtBefore(any())).thenReturn(List.of());

        service.deleteExpiredAccounts();

        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(userRepository).findByDeletionRequestedAtBefore(thresholdCaptor.capture());

        Instant expected = Instant.now().minus(30, ChronoUnit.DAYS);
        assertThat(thresholdCaptor.getValue()).isCloseTo(expected, org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
    }

    private User umUsuarioComDeletionRequestedAt(Instant deletionRequestedAt) {
        User user = new User();
        user.setName("Usuário Excluído");
        user.setEmail(user.getId() + "@email.com");
        user.setPasswordHash("hash");
        user.setDeletionRequestedAt(deletionRequestedAt);
        user.setCreatedAt(Instant.now().minus(60, ChronoUnit.DAYS));
        user.setUpdatedAt(Instant.now().minus(60, ChronoUnit.DAYS));
        return user;
    }
}
