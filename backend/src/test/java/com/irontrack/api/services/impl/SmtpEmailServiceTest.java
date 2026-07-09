package com.irontrack.api.services.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 7: templates de verificação/reset com
 * deep link `irontrack://...`, e comportamento de "logar em vez de enviar"
 * quando `SMTP_HOST` está vazio (perfil dev sem provedor real configurado).
 */
class SmtpEmailServiceTest {

    private static final String FROM_ADDRESS = "no-reply@irontrack.app";

    @Test
    void deveEnviarEmailDeVerificacaoComDeepLinkQuandoSmtpConfigurado() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailService service = new SmtpEmailService(mailSender, FROM_ADDRESS, "smtp.example.com");

        service.sendVerificationEmail("gabriel.silva@email.com", "Gabriel Silva", "raw-token-abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("gabriel.silva@email.com");
        assertThat(message.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(message.getSubject()).contains("Confirme");
        assertThat(message.getText()).contains("irontrack://verify-email/raw-token-abc");
    }

    @Test
    void deveEnviarEmailDeResetComDeepLinkQuandoSmtpConfigurado() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailService service = new SmtpEmailService(mailSender, FROM_ADDRESS, "smtp.example.com");

        service.sendPasswordResetEmail("gabriel.silva@email.com", "raw-reset-token-xyz");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("gabriel.silva@email.com");
        assertThat(message.getText()).contains("irontrack://reset-password/raw-reset-token-xyz");
    }

    @Test
    void naoDeveEnviarEmailQuandoSmtpHostEstiverVazio() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailService service = new SmtpEmailService(mailSender, FROM_ADDRESS, "");

        service.sendVerificationEmail("gabriel.silva@email.com", "Gabriel Silva", "raw-token-abc");
        service.sendPasswordResetEmail("gabriel.silva@email.com", "raw-reset-token-xyz");

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }
}
