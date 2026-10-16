package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.AttendanceMark;
import com.uniquindio.teen_sport.dto.AttendanceRoster;
import com.uniquindio.teen_sport.dto.RecordAttendanceRequest;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.AttendanceRecord;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.FieldValidationException;
import com.uniquindio.teen_sport.mapper.AttendanceMapper;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista de asistencia de una sesión (A7) y registro de la asistencia (A8).
 *
 * <p>La lista son los deportistas con matrícula {@code ACTIVE} en el grupo iniciada en el mes de la
 * sesión o antes, ordenados por apellidos. Las marcas de deportistas que ya no están en la lista (por
 * ejemplo, porque cancelaron después) se conservan en el historial, pero no aparecen en la lista.</p>
 *
 * <p>Registrar bloquea la sesión: dos registros simultáneos se serializan y el segundo actualiza las
 * marcas del primero en lugar de duplicarlas.</p>
 */
@Service
public class AttendanceService {

    static final String NOT_FOUND_MESSAGE = "La sesión no existe.";
    static final String CANCELLED_MESSAGE = "La sesión está cancelada.";
    static final String FUTURE_MESSAGE = "No se puede registrar la asistencia de una sesión futura.";
    static final String DUPLICATE_MARK_MESSAGE = "Cada deportista debe aparecer una sola vez.";
    static final String NOT_IN_ROSTER_MESSAGE = "Un deportista no pertenece a la lista de asistencia de la sesión.";
    static final String INCOMPLETE_MESSAGE = "Debe marcar la asistencia de todos los deportistas de la lista.";

    private final TrainingSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupAccessPolicy accessPolicy;
    private final AcademyCalendar calendar;

    /**
     * @param sessionRepository    sesiones
     * @param attendanceRepository marcas de asistencia
     * @param enrollmentRepository matrículas, para la lista de asistencia
     * @param accessPolicy         regla de propiedad de los grupos
     * @param calendar             fecha y hora de la academia
     */
    public AttendanceService(TrainingSessionRepository sessionRepository,
                             AttendanceRecordRepository attendanceRepository,
                             EnrollmentRepository enrollmentRepository, GroupAccessPolicy accessPolicy,
                             AcademyCalendar calendar) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.accessPolicy = accessPolicy;
        this.calendar = calendar;
    }

    /**
     * A7: lista de asistencia con las marcas guardadas.
     *
     * @param user      administrador o entrenador del grupo
     * @param sessionId id de la sesión
     * @return la lista; {@code status} es {@code null} para los deportistas sin marca
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED}
     */
    @Transactional(readOnly = true)
    public AttendanceRoster roster(AuthenticatedUser user, Integer sessionId) {
        TrainingSession session = sessionRepository.findDetailedById(sessionId).orElseThrow(AttendanceService::notFound);
        accessPolicy.requireAccess(user, session.getGroup());
        Map<Integer, AttendanceStatus> marks = attendanceRepository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(attendance -> attendance.getAthlete().getId(), AttendanceRecord::getStatus));
        return AttendanceMapper.toRoster(session, rosterOf(session), marks);
    }

    /**
     * A8: guarda la asistencia de toda la lista y deja la sesión {@code COMPLETED}. Guardar de nuevo
     * reemplaza las marcas anteriores. Una sesión de hoy se puede registrar aunque aún no haya empezado.
     *
     * @param user      entrenador a cargo del grupo
     * @param sessionId id de la sesión
     * @param request   una marca por deportista de la lista
     * @return la lista con las marcas guardadas
     * @throws BusinessException        404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED}; 409 {@code SESSION_CANCELLED};
     *                                  422 {@code FUTURE_SESSION}, {@code ATHLETE_NOT_IN_ROSTER} o {@code INCOMPLETE_ATTENDANCE}
     * @throws FieldValidationException 400 si un deportista aparece más de una vez
     */
    @Transactional
    public AttendanceRoster recordAttendance(AuthenticatedUser user, Integer sessionId,
                                             RecordAttendanceRequest request) {
        TrainingSession session = sessionRepository.findLockedById(sessionId).orElseThrow(AttendanceService::notFound);
        accessPolicy.requireCoachOf(user, session.getGroup());
        requireRecordable(session);
        Map<Integer, AttendanceStatus> marks = marksOf(request);
        List<Athlete> roster = rosterOf(session);
        requireWholeRoster(marks.keySet(), roster);
        save(session, roster, marks);
        session.setStatus(SessionStatus.COMPLETED);
        return AttendanceMapper.toRoster(session, roster, marks);
    }

    private void requireRecordable(TrainingSession session) {
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.SESSION_CANCELLED, HttpStatus.CONFLICT, CANCELLED_MESSAGE);
        }
        if (session.getSessionDate().isAfter(calendar.today())) {
            throw new BusinessException(ErrorCode.FUTURE_SESSION, HttpStatus.UNPROCESSABLE_CONTENT, FUTURE_MESSAGE);
        }
    }

    private static Map<Integer, AttendanceStatus> marksOf(RecordAttendanceRequest request) {
        Map<Integer, AttendanceStatus> marks = new HashMap<>();
        for (AttendanceMark mark : request.records()) {
            if (marks.put(mark.athleteId(), mark.status()) != null) {
                throw new FieldValidationException("records", DUPLICATE_MARK_MESSAGE);
            }
        }
        return marks;
    }

    private static void requireWholeRoster(Set<Integer> markedIds, List<Athlete> roster) {
        Set<Integer> rosterIds = roster.stream().map(Athlete::getId).collect(Collectors.toSet());
        if (!rosterIds.containsAll(markedIds)) {
            throw new BusinessException(ErrorCode.ATHLETE_NOT_IN_ROSTER, HttpStatus.UNPROCESSABLE_CONTENT,
                    NOT_IN_ROSTER_MESSAGE);
        }
        if (markedIds.size() < rosterIds.size()) {
            throw new BusinessException(ErrorCode.INCOMPLETE_ATTENDANCE, HttpStatus.UNPROCESSABLE_CONTENT,
                    INCOMPLETE_MESSAGE);
        }
    }

    private void save(TrainingSession session, List<Athlete> roster, Map<Integer, AttendanceStatus> marks) {
        Instant now = calendar.now();
        Map<Integer, AttendanceRecord> saved = attendanceRepository.findBySessionId(session.getId()).stream()
                .collect(Collectors.toMap(attendance -> attendance.getAthlete().getId(), Function.identity()));
        List<AttendanceRecord> created = new ArrayList<>();
        for (Athlete athlete : roster) {
            AttendanceStatus status = marks.get(athlete.getId());
            AttendanceRecord attendance = saved.get(athlete.getId());
            if (attendance == null) {
                created.add(new AttendanceRecord(session, athlete, status, now));
            } else {
                attendance.mark(status, now);
            }
        }
        attendanceRepository.saveAll(created);
    }

    private List<Athlete> rosterOf(TrainingSession session) {
        return enrollmentRepository.findRosterAthletes(session.getGroup().getId(),
                YearMonth.from(session.getSessionDate()).toString());
    }

    private static BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE);
    }
}
