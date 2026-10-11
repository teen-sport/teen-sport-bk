package com.uniquindio.teen_sport.validation;

/**
 * Normalización de textos libres antes de guardarlos.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * @param value texto recibido, posiblemente nulo o en blanco
     * @return el texto sin espacios alrededor, o {@code null} si no tiene contenido
     */
    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
