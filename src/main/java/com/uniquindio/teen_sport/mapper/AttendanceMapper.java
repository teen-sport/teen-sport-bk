package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.AthleteRef;
import com.uniquindio.teen_sport.dto.AttendanceRecordResponse;
import com.uniquindio.teen_sport.dto.AttendanceRoster;
import com.uniquindio.teen_sport.dto.AttendanceRosterEntry;
import com.uniquindio.teen_sport.dto.GuardianContact;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.AttendanceRecord;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.validation.AthleteAgePolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Convierte listas y marcas de asistencia al formato del contrato. En el historial, del deportista
 * solo expone su id, nombres y apellidos; la lista de asistencia añade su edad y el contacto de su
 * acudiente para emergencias. Son funciones puras, sin estado ni dependencias.
 */
public final class AttendanceMapper {

    private AttendanceMapper() {
    }

    /**
     * @param session  sesión con su grupo cargado
     * @param athletes lista de asistencia, ya ordenada, con el acudiente y su cuenta cargados
     * @param marks    marcas guardadas por id de deportista
     * @param today    fecha actual de la academia, para la edad
     * @return la lista con la marca de cada deportista, o {@code null} si aún no la tiene
     */
    public static AttendanceRoster toRoster(TrainingSession session, List<Athlete> athletes,
                                            Map<Integer, AttendanceStatus> marks, LocalDate today) {
        List<AttendanceRosterEntry> entries = athletes.stream()
                .map(athlete -> new AttendanceRosterEntry(athlete.getId(), athlete.getFirstName(),
                        athlete.getLastName(), AthleteAgePolicy.ageOn(athlete.getBirthDate(), today),
                        guardianContact(athlete), marks.get(athlete.getId())))
                .toList();
        return new AttendanceRoster(session.getId(), SessionMapper.group(session), session.getSessionDate(),
                session.getStatus(), SessionMapper.attendanceRecorded(session), entries);
    }

    private static GuardianContact guardianContact(Athlete athlete) {
        User account = athlete.getGuardian().getUser();
        return new GuardianContact(account.getFirstName(), account.getLastName(), account.getPhone());
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
