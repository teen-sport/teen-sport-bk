package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.AttendanceStatus;
import java.time.LocalDate;

/**
 * Filtros del historial de asistencia (A9). {@code coachId} y {@code guardianId} limitan el alcance según
 * el rol y los fija el servicio, nunca el cliente.
 *
 * @param athleteId  filtro por deportista
 * @param groupId    filtro por grupo
 * @param from       primera fecha de sesión, incluida
 * @param to         última fecha de sesión, incluida
 * @param status     filtro por marca
 * @param coachId    entrenador del grupo al que se limita la consulta, o {@code null}
 * @param guardianId acudiente al que se limita la consulta, o {@code null}
 */
public record AttendanceFilter(Integer athleteId, Integer groupId, LocalDate from, LocalDate to,
                               AttendanceStatus status, Integer coachId, Integer guardianId) {

    /**
     * @param scopedCoachId    entrenador al que se limita la consulta
     * @param scopedGuardianId acudiente al que se limita la consulta
     * @return el mismo filtro con el alcance del rol
     */
    public AttendanceFilter withScope(Integer scopedCoachId, Integer scopedGuardianId) {
        return new AttendanceFilter(athleteId, groupId, from, to, status, scopedCoachId, scopedGuardianId);
    }
}
