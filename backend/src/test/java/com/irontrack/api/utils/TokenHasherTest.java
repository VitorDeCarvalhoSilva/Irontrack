package com.irontrack.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Geração de token bruto de alta entropia (e-mail de verificação/reset de
 * senha) e seu hash estável para persistência (02_SCHEMA_SQLITE.md §2 —
 * "sempre hash, nunca o token em texto plano").
 */
class TokenHasherTest {

    @Test
    void deveGerarHashSempreIgualParaOMesmoToken() {
        String hash1 = TokenHasher.hash("mesmo-token");
        String hash2 = TokenHasher.hash("mesmo-token");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void deveGerarHashesDiferentesParaTokensDiferentes() {
        String hash1 = TokenHasher.hash("token-a");
        String hash2 = TokenHasher.hash("token-b");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hashNuncaDeveSerIgualAoTokenOriginal() {
        String rawToken = "token-em-texto-plano";

        assertThat(TokenHasher.hash(rawToken)).isNotEqualTo(rawToken);
    }

    @Test
    void deveGerarTokensBrutosUnicosACadaChamada() {
        String token1 = TokenHasher.generateRawToken();
        String token2 = TokenHasher.generateRawToken();

        assertThat(token1).isNotEqualTo(token2);
        assertThat(token1).isNotBlank();
    }
}
