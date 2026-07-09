package com.irontrack.api.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Ponte entre {@link Instant} (usado nas entidades para permitir aritmética
 * de datas segura, ex: expiração de token/carência de exclusão) e o formato
 * TEXT ISO8601 UTC com milissegundos exigido por 02_SCHEMA_SQLITE.md §2
 * (`YYYY-MM-DDThh:mm:ss.sssZ`). {@code autoApply = true} aplica a conversão
 * automaticamente a todo campo {@link Instant} das entidades JPA.
 */
@Converter(autoApply = true)
public class InstantIso8601Converter implements AttributeConverter<Instant, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : FORMATTER.format(attribute);
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Instant.parse(dbData);
    }
}
