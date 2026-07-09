package com.irontrack.api.services.impl;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração contra um servidor SMTP real embarcado (GreenMail,
 * 10_ESTRATEGIA_DE_TESTES.md §C) - confirma que {@link SmtpEmailService}
 * de fato entrega a mensagem quando um host SMTP está configurado, não
 * apenas que o mock foi chamado (coberto por {@link SmtpEmailServiceTest}).
 */
class SmtpEmailServiceIT {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void deveEntregarEmailDeVerificacaoNoServidorSmtpDeTeste() throws Exception {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(greenMail.getSmtp().getPort());

        SmtpEmailService service = new SmtpEmailService(mailSender, "no-reply@irontrack.app", "localhost");

        service.sendVerificationEmail("gabriel.silva@email.com", "Gabriel Silva", "raw-token-it");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("gabriel.silva@email.com");
        assertThat(received[0].getSubject()).contains("Confirme");
        assertThat((String) received[0].getContent()).contains("irontrack://verify-email/raw-token-it");
    }
}
