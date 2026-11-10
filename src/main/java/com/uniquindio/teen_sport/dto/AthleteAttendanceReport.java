package com.uniquindio.teen_sport.dto;

import java.util.List;

/**
 * Asistencia de un deportista (R3).
 *
 * @param athleteId        id del deportista
 * @param recordedSessions sesiones con marca del deportista
 * @param present          marcas {@code PRESENT}
 * @param absent           marcas {@code ABSENT}
 * @param attendanceRate   porcentaje de marcas {@code PRESENT}; {@code null} si no hay marcas
 * @param byGroup          marcas por grupo
 */
public record AthleteAttendanceReport(Integer athleteId, long recordedSessions, long present, long absent,
                                      Double attendanceRate, List<GroupMarks> byGroup) {

    /**
     * @param groupId        id del grupo
     * @param groupName      nombre del grupo
     * @param present        marcas {@code PRESENT}
     * @param absent         marcas {@code ABSENT}
     * @param attendanceRate porcentaje de marcas {@code PRESENT}; {@code null} si no hay marcas
     */
    public record GroupMarks(Integer groupId, String groupName, long present, long absent, Double attendanceRate) {
    }
}
