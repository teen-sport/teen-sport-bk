package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.AthleteRef;
import com.uniquindio.teen_sport.dto.AttendanceRecordResponse;
import com.uniquindio.teen_sport.dto.AttendanceRoster;
import com.uniquindio.teen_sport.dto.AttendanceRosterEntry;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.AttendanceRecord;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import java.util.List;
import java.util.Map;

/**
 * Convierte listas y marcas de asistencia al formato del contrato. Del deportista solo expone su id,
 * nombres y apellidos. Son funciones puras, sin estado ni dependencias.
 */
public final class AttendanceMapper {

    private AttendanceMapper() {
    }

    /**
     * @param session  sesión con su grupo cargado
     * @param athletes lista de asistencia, ya ordenada
     * @param marks    marcas guardadas por id de deportista
     * @return la lista con la marca de cada deportista, o {@code null} si aún no la tiene
     */
    public static AttendanceRoster toRoster(TrainingSession session, List<Athlete> athletes,
                                            Map<Integer, AttendanceStatus> marks) {
        List<AttendanceRosterEntry> entries = athletes.stream()
                .map(athlete -> new AttendanceRosterEntry(athlete.getId(), athlete.getFirstName(),
                        athlete.getLastName(), marks.get(athlete.getId())))
                .toList();
        return new AttendanceRoster(session.getId(), SessionMapper.group(session), session.getSessionDate(),
                session.getStatus(), SessionMapper.attendanceRecorded(session), entries);
    }

    /**
     * @param attendance marca con sesión, grupo, disciplina y deportista cargados
     * @return la marca del historial
     */
    public static AttendanceRecordResponse toResponse(AttendanceRecord attendance) {
        TrainingSession session = attendance.getSession();
        Athlete athlete = attendance.getAthlete();
        return new AttendanceRecordResponse(attendance.getId(), session.getId(), session.getSessionDate(),
                SessionMapper.group(session), SessionMapper.discipline(session),
                new AthleteRef(athlete.getId(), athlete.getFirstName(), athlete.getLastName()),
                attendance.getStatus(), attendance.getRecordedAt());
    }
}
