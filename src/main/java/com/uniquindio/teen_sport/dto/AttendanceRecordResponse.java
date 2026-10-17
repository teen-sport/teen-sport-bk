package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Marca del historial de asistencia (esquema {@code AttendanceRecord}).
 *
 * @param id         id
 * @param sessionId  id de la sesión
 * @param date       fecha de la sesión
 * @param group      grupo
 * @param discipline disciplina del grupo
 * @param athlete    deportista
 * @param status     marca
 * @param recordedAt último registro de la marca
 */
public record AttendanceRecordResponse(Integer id, Integer sessionId, LocalDate date, IdName group,
                                       IdName discipline, AthleteRef athlete, AttendanceStatus status,
                                       Instant recordedAt) {
}
