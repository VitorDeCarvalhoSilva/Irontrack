package com.irontrack.api.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Geração de token bruto de alta entropia e hash estável (SHA-256) para
 * persistência de tokens transacionais de curta duração (verificação de
 * e-mail, reset de senha — 02_SCHEMA_SQLITE.md §2: "sempre hash, nunca o
 * token em texto plano"). SHA-256 (não BCrypt) é suficiente aqui: os tokens
 * já têm alta entropia própria e vida curta, dispensando o custo
 * computacional deliberado de um hash lento de senha.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String generateRawToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM.", e);
        }
    }
}
