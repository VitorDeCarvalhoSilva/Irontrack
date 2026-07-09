package com.irontrack.api.services;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 7 — envio de e-mails transacionais de
 * verificação de cadastro e redefinição de senha.
 */
public interface EmailService {

    void sendVerificationEmail(String toEmail, String toName, String rawVerificationToken);

    void sendPasswordResetEmail(String toEmail, String rawResetToken);
}
