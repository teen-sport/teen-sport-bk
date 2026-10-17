package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;

/**
 * Solicitud de matrícula (esquema {@code CreateEnrollmentRequest}).
 *
 * @param athleteId deportista
 * @param groupId   grupo
 * @param period    mes de inicio, {@code YYYY-MM}
 */
public record CreateEnrollmentRequest(
        @NotNull Integer athleteId,
        @NotNull Integer groupId,
        @NotNull YearMonth period) {
}
