package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.IdName;
import com.uniquindio.teen_sport.dto.PersonRef;
import com.uniquindio.teen_sport.dto.SessionAthlete;
import com.uniquindio.teen_sport.dto.SessionResponse;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.entity.User;
import java.util.List;

/**
 * Convierte sesiones al formato del contrato. Del entrenador solo expone su id y su nombre. Son
 * funciones puras, sin estado ni dependencias.
 */
public final class SessionMapper {

    private SessionMapper() {
    }

    /**
     * @param session sesión con grupo, disciplina, entrenador y cuenta del entrenador cargados
     * @return la sesión, sin la lista de deportistas
     */
    public static SessionResponse toResponse(TrainingSession session) {
        return toResponse(session, null);
    }

    /**
     * @param session  sesión con grupo, disciplina, entrenador y cuenta del entrenador cargados
     * @param athletes deportistas del acudiente que asisten, o {@code null} si quien consulta no es acudiente
     * @return la sesión
     */
    public static SessionResponse toResponse(TrainingSession session, List<SessionAthlete> athletes) {
        User coachUser = session.getCoach().getUser();
        return new SessionResponse(
                session.getId(),
                group(session),
                discipline(session),
                new PersonRef(session.getCoach().getId(), coachUser.getFirstName() + " " + coachUser.getLastName()),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                attendanceRecorded(session),
                session.getNotes(),
                athletes);
    }

    /**
     * La asistencia queda registrada justo cuando la sesión pasa a {@code COMPLETED} (A8).
     *
     * @param session sesión
     * @return si la sesión ya tiene la asistencia registrada
     */
    static boolean attendanceRecorded(TrainingSession session) {
        return session.getStatus() == SessionStatus.COMPLETED;
    }

    static IdName group(TrainingSession session) {
        Group group = session.getGroup();
        return new IdName(group.getId(), group.getName());
    }

    static IdName discipline(TrainingSession session) {
        Discipline discipline = session.getGroup().getDiscipline();
        return new IdName(discipline.getId(), discipline.getName());
    }
}
