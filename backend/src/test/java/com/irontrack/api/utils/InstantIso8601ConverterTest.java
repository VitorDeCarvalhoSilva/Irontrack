package com.irontrack.api.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 02_SCHEMA_SQLITE.md §2: timestamps são armazenados como TEXT ISO8601 UTC
 * (`YYYY-MM-DDThh:mm:ss.sssZ`) - este converter é a única ponte entre
 * `java.time.Instant` (usado nas entidades para permitir aritmética de datas
 * segura, ex: expiração de token) e o formato TEXT exato do schema.
 */
class InstantIso8601ConverterTest {

    private final InstantIso8601Converter converter = new InstantIso8601Converter();

    @Test
    void deveConverterInstantParaTextoIso8601ComMilissegundos() {
        Instant instant = Instant.parse("2026-07-01T15:30:00.123Z");

        String result = converter.convertToDatabaseColumn(instant);

        assertThat(result).isEqualTo("2026-07-01T15:30:00.123Z");
    }

    @Test
    void deveConverterInstantSemMilissegundosPreenchendoComZeros() {
        Instant instant = Instant.parse("2026-07-01T15:30:00Z");

        String result = converter.convertToDatabaseColumn(instant);

        assertThat(result).isEqualTo("2026-07-01T15:30:00.000Z");
    }

    @Test
    void deveRetornarNuloAoConverterInstantNuloParaColuna() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void deveConverterTextoIso8601ParaInstant() {
        Instant result = converter.convertToEntityAttribute("2026-07-01T15:30:00.123Z");

        assertThat(result).isEqualTo(Instant.parse("2026-07-01T15:30:00.123Z"));
    }

    @Test
    void deveRetornarNuloAoConverterColunaNulaParaInstant() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void deveFazerRoundTripMantendoOMesmoInstant() {
        Instant original = Instant.parse("2026-01-15T08:05:09.007Z");

        String asText = converter.convertToDatabaseColumn(original);
        Instant restored = converter.convertToEntityAttribute(asText);

        assertThat(restored).isEqualTo(original);
    }
}
