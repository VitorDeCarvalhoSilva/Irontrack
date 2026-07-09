package com.irontrack.api.dto.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regra de força de senha (00_PRD_IRONTRACK.md §4.3 / 03_CONTRATOS_API.md
 * §2.7): mínimo 8 caracteres, contendo letras e números.
 */
class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void deveAceitarSenhaComLetrasNumerosEOitoCaracteres() {
        assertThat(validator.isValid("Senha123", null)).isTrue();
    }

    @Test
    void deveRejeitarSenhaComMenosDeOitoCaracteres() {
        assertThat(validator.isValid("Sen12", null)).isFalse();
    }

    @Test
    void deveRejeitarSenhaSemNumero() {
        assertThat(validator.isValid("SenhaSegura", null)).isFalse();
    }

    @Test
    void deveRejeitarSenhaSemLetra() {
        assertThat(validator.isValid("12345678", null)).isFalse();
    }

    @Test
    void deveAceitarValorNuloDelegandoObrigatoriedadeParaNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
