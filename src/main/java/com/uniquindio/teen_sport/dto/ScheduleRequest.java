package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Creación o edición de una franja semanal (esquema {@code ScheduleRequest}). Las horas llegan como
 * {@code HH:mm}; un formato distinto responde 400.
 *
 * @param dayOfWeek día de la semana ({@code MONDAY} a {@code SUNDAY})
 * @param startTime hora de inicio
 * @param endTime   hora de fin, posterior al inicio
 */
public record ScheduleRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
}
