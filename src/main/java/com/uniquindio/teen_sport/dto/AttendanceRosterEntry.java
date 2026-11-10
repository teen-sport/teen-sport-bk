package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.AttendanceStatus;

/**
 * Deportista de la lista de asistencia (esquema {@code AttendanceRosterEntry}).
 *
 * @param athleteId  id del deportista
 * @param firstNames nombres
 * @param lastNames  apellidos
 * @param age        edad en años a la fecha actual de la academia, no a la de la sesión
 * @param guardian   contacto del acudiente para emergencias
 * @param status     marca guardada, o {@code null} si aún no se registró
 */
public record AttendanceRosterEntry(Integer athleteId, String firstNames, String lastNames, int age,
                                    GuardianContact guardian, AttendanceStatus status) {
}
