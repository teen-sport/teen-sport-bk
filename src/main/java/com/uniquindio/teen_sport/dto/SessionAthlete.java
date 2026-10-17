package com.uniquindio.teen_sport.dto;

/**
 * Deportista de un acudiente que asiste a una sesión (elemento de {@code TrainingSession.athletes}).
 *
 * @param id         id del deportista
 * @param firstNames nombres
 */
public record SessionAthlete(Integer id, String firstNames) {
}
