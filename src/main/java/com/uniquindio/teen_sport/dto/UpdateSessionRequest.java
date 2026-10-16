package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Nueva fecha y horas de una sesión (esquema {@code UpdateSessionRequest}). Las notas reemplazan las
 * anteriores; si no se envían, quedan vacías.
 *
 * @param date      fecha, no anterior a hoy
 * @param startTime hora de inicio
 * @param endTime   hora de fin, posterior al inicio
 * @param notes     notas opcionales, hasta 500 caracteres
 */
public record UpdateSessionRequest(
        @NotNull LocalDate date,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @Size(max = 500) String notes) {
}
