package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Asignación del entrenador de un grupo (esquema {@code AssignCoachRequest}).
 *
 * @param coachId id del entrenador
 */
public record AssignCoachRequest(@NotNull Integer coachId) {
}
