package com.uniquindio.teen_sport.dto;

import java.util.List;

/**
 * Indicadores de asistencia de un rango de fechas (R2).
 *
 * @param scheduledSessions sesiones no canceladas con fecha hasta hoy
 * @param completedSessions sesiones con la asistencia registrada
 * @param recordingCoverage porcentaje de sesiones realizadas sobre las programadas (RNF-10)
 * @param attendanceRate    porcentaje de marcas {@code PRESENT}; {@code null} si no hay marcas
 * @param byGroup           indicadores por grupo
 */
public record AttendanceReport(long scheduledSessions, long completedSessions, double recordingCoverage,
                               Double attendanceRate, List<GroupAttendance> byGroup) {

    /**
     * @param groupId           id del grupo
     * @param groupName         nombre del grupo
     * @param discipline        nombre de la disciplina
     * @param scheduledSessions sesiones no canceladas con fecha hasta hoy
     * @param completedSessions sesiones con la asistencia registrada
     * @param attendanceRate    porcentaje de marcas {@code PRESENT}; {@code null} si no hay marcas
     */
    public record GroupAttendance(Integer groupId, String groupName, String discipline, long scheduledSessions,
                                  long completedSessions, Double attendanceRate) {
    }
}
