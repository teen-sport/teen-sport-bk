package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniquindio.teen_sport.entity.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Sesión de entrenamiento (esquema {@code TrainingSession}). Las horas se expresan como {@code HH:mm}.
 *
 * @param id                 id
 * @param group              grupo
 * @param discipline         disciplina del grupo
 * @param coach              entrenador que dicta la sesión
 * @param date               fecha
 * @param startTime          hora de inicio
 * @param endTime            hora de fin
 * @param status             estado
 * @param attendanceRecorded si ya se registró la asistencia
 * @param notes              notas; en una sesión cancelada, el motivo
 * @param athletes           solo para un acudiente: sus deportistas que asisten a la sesión
 */
public record SessionResponse(
        Integer id,
        IdName group,
        IdName discipline,
        PersonRef coach,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        SessionStatus status,
        boolean attendanceRecorded,
        String notes,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<SessionAthlete> athletes) {
}
