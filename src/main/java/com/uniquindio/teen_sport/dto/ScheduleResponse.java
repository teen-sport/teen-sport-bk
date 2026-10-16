package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Franja semanal de un grupo (esquema {@code Schedule}). Las horas se expresan como {@code HH:mm}.
 *
 * @param id        id
 * @param dayOfWeek día de la semana
 * @param startTime hora de inicio
 * @param endTime   hora de fin
 */
public record ScheduleResponse(
        Integer id,
        DayOfWeek dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
}
