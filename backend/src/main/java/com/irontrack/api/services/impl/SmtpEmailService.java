package com.irontrack.api.services.impl;

import com.irontrack.api.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementação SMTP de {@link EmailService} (07_ROADMAP_BACKEND.md §C.1,
 * item 7). Se {@code SMTP_HOST} estiver vazio (perfil dev sem provedor real
 * configurado, 05_DEVOPS_E_SEGURANCA.md §D), apenas loga a mensagem em vez
 * de enviar de fato — nunca falha silenciosamente nem lança exceção por
 * ausência de configuração de e-mail em desenvolvimento.
 */
@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String smtpHost;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${irontrack.mail.from-address}") String fromAddress,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.smtpHost = smtpHost;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String toName, String rawVerificationToken) {
        String subject = "Confirme seu e-mail — IronTrack";
        String body = "Olá, " + toName + "!\n\n"
                + "Confirme seu cadastro no IronTrack acessando o link abaixo pelo app:\n\n"
                + "irontrack://verify-email/" + rawVerificationToken + "\n\n"
                + "Se você não se cadastrou, ignore este e-mail.";
        send(toEmail, subject, body);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String rawResetToken) {
        String subject = "Redefinição de senha — IronTrack";
        String body = "Olá!\n\n"
                + "Recebemos uma solicitação de redefinição de senha. Acesse o link abaixo pelo app "
                + "(válido por 1 hora):\n\n"
                + "irontrack://reset-password/" + rawResetToken + "\n\n"
                + "Se você não solicitou, ignore este e-mail.";
        send(toEmail, subject, body);
    }

    private void send(String toEmail, String subject, String body) {
        if (smtpHost == null || smtpHost.isBlank()) {
            log.info("SMTP_HOST não configurado - e-mail não enviado, apenas logado. to={}, subject={}, body={}",
                    toEmail, subject, body);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
