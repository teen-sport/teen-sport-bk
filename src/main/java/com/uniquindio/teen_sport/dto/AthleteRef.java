package com.uniquindio.teen_sport.dto;

/**
 * Referencia breve a un deportista (esquema {@code AthleteRef}).
 *
 * @param id         id del deportista
 * @param firstNames nombres
 * @param lastNames  apellidos
 */
public record AthleteRef(Integer id, String firstNames, String lastNames) {
}
