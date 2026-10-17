package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.SessionStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Lista de asistencia de una sesión con sus marcas (esquema {@code AttendanceRoster}).
 *
 * @param sessionId          id de la sesión
 * @param group              grupo
 * @param date               fecha de la sesión
 * @param status             estado de la sesión
 * @param attendanceRecorded si ya se registró la asistencia
 * @param records            deportistas ordenados por apellidos
 */
public record AttendanceRoster(Integer sessionId, IdName group, LocalDate date, SessionStatus status,
                               boolean attendanceRecorded, List<AttendanceRosterEntry> records) {
}
