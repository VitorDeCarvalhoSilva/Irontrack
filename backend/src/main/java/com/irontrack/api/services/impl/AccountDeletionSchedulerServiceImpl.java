package com.irontrack.api.services.impl;

import com.irontrack.api.entities.User;
import com.irontrack.api.repositories.UserRepository;
import com.irontrack.api.services.AccountDeletionSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Job diário (07_ROADMAP_BACKEND.md §C.1, item 5) que varre {@code users}
 * com {@code deletion_requested_at} mais antigo que 30 dias e executa a
 * exclusão física — a cascata via FKs já definidas em 02_SCHEMA_SQLITE.md
 * remove automaticamente todo o histórico de treino associado
 * (11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md §B.4).
 */
@Service
public class AccountDeletionSchedulerServiceImpl implements AccountDeletionSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionSchedulerServiceImpl.class);
    private static final Duration GRACE_PERIOD = Duration.ofDays(30);

    private final UserRepository userRepository;

    public AccountDeletionSchedulerServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredAccounts() {
        Instant threshold = Instant.now().minus(GRACE_PERIOD);
        List<User> expiredAccounts = userRepository.findByDeletionRequestedAtBefore(threshold);

        for (User user : expiredAccounts) {
            userRepository.delete(user);
            log.info("Conta {} excluída fisicamente após período de carência de 30 dias.", user.getId());
        }
    }
}
