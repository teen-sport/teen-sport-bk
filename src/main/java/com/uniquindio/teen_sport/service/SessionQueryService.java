package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.SessionAthlete;
import com.uniquindio.teen_sport.dto.SessionResponse;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.SecurityMessages;
import com.uniquindio.teen_sport.mapper.SessionMapper;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.SessionFilter;
import com.uniquindio.teen_sport.repository.SessionSpecifications;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.DateRangePolicy;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calendario de sesiones (A3) y detalle de una sesión (A4).
 *
 * <p>Alcance por rol: el administrador ve todas; un entrenador, las de los grupos que tiene a cargo; un
 * acudiente, las de los grupos donde uno de sus deportistas tiene una matrícula {@code ACTIVE} iniciada
 * en el mes de la sesión o antes, igual que la lista de asistencia (A7). Al acudiente se le indica en cada
 * sesión cuáles de sus deportistas asisten.</p>
 *
 * <p>Rango de A3: sin fechas, la semana actual de lunes a domingo; con solo {@code from}, siete días desde
 * esa fecha; con solo {@code to}, los siete días que terminan en ella. El rango abarca como máximo
 * {@value DateRangePolicy#MAX_SESSION_RANGE_DAYS} días.</p>
 */
@Service
public class SessionQueryService {

    static final String NOT_FOUND_MESSAGE = "La sesión no existe.";

    /** Días del rango cuando solo se indica uno de sus extremos. */
    static final int DEFAULT_RANGE_DAYS = 7;

    /** Orden del calendario: fecha, hora de inicio e id para desempatar. */
    static final Sort ORDER = Sort.by("sessionDate", "startTime", "id");

    private static final Comparator<SessionAthlete> ATHLETE_ORDER =
            Comparator.comparing(SessionAthlete::firstNames).thenComparing(SessionAthlete::id);

    private final TrainingSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupAccessPolicy groupAccessPolicy;
    private final AthleteAccessPolicy athleteAccessPolicy;
    private final AcademyCalendar calendar;

    /**
     * @param sessionRepository   sesiones
     * @param enrollmentRepository matrículas, para el alcance del acudiente y el filtro por deportista
     * @param groupAccessPolicy   regla de propiedad de los grupos
     * @param athleteAccessPolicy regla de propiedad de los deportistas
     * @param calendar            fecha de la academia
     */
    public SessionQueryService(TrainingSessionRepository sessionRepository, EnrollmentRepository enrollmentRepository,
                               GroupAccessPolicy groupAccessPolicy, AthleteAccessPolicy athleteAccessPolicy,
                               AcademyCalendar calendar) {
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.groupAccessPolicy = groupAccessPolicy;
        this.athleteAccessPolicy = athleteAccessPolicy;
        this.calendar = calendar;
    }

    /**
     * A3: sesiones del rango dentro del alcance del rol.
     *
     * @param user      identidad autenticada
     * @param query     rango y filtros; su alcance ({@code coachId}, {@code groupIds}) se ignora y lo fija el rol
     * @param athleteId filtro por deportista: sesiones de grupos donde tiene una matrícula {@code ACTIVE} ya iniciada
     * @return las sesiones ordenadas por fecha y hora de inicio
     * @throws BusinessException 422 {@code INVALID_DATE_RANGE}
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> list(AuthenticatedUser user, SessionFilter query, Integer athleteId) {
        LocalDate from = rangeStart(query.from(), query.to());
        LocalDate to = query.to() != null ? query.to() : from.plusDays(DEFAULT_RANGE_DAYS - 1L);
        DateRangePolicy.requireSessionRange(from, to);
        boolean guardian = user.role() == RoleName.GUARDIAN;
        List<Enrollment> enrollments = scopedEnrollments(user, athleteId);
        if (enrollments != null && enrollments.isEmpty()) {
            return List.of();
        }
        Set<Integer> groupIds = enrollments == null ? null
                : enrollments.stream().map(enrollment -> enrollment.getGroup().getId()).collect(Collectors.toSet());
        Integer coachId = guardian ? null : groupAccessPolicy.coachScope(user, null);
        SessionFilter filter = new SessionFilter(from, to, query.groupId(), query.disciplineId(), query.status(),
                coachId, groupIds);
        List<SessionResponse> sessions = new ArrayList<>();
        for (TrainingSession session : sessionRepository.findAll(SessionSpecifications.sessions(filter), ORDER)) {
            List<SessionAthlete> athletes = enrollments == null ? null : attending(enrollments, session);
            if (athletes == null || !athletes.isEmpty()) {
                sessions.add(SessionMapper.toResponse(session, guardian ? athletes : null));
            }
        }
        return sessions;
    }

    /**
     * A4: una sesión dentro del alcance del rol.
     *
     * @param user      identidad autenticada
     * @param sessionId id de la sesión
     * @return la sesión; para un acudiente, con sus deportistas que asisten
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED} si está fuera del alcance
     */
    @Transactional(readOnly = true)
    public SessionResponse get(AuthenticatedUser user, Integer sessionId) {
        TrainingSession session = sessionRepository.findDetailedById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
        if (user.role() != RoleName.GUARDIAN) {
            groupAccessPolicy.requireAccess(user, session.getGroup());
            return SessionMapper.toResponse(session);
        }
        List<SessionAthlete> athletes = attending(guardianEnrollments(user), session);
        if (athletes.isEmpty()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, SecurityMessages.ACCESS_DENIED);
        }
        return SessionMapper.toResponse(session, athletes);
    }

    private LocalDate rangeStart(LocalDate from, LocalDate to) {
        if (from != null) {
            return from;
        }
        if (to != null) {
            return to.minusDays(DEFAULT_RANGE_DAYS - 1L);
        }
        return calendar.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * @return las matrículas {@code ACTIVE} que limitan la consulta, o {@code null} si no la limitan
     */
    private List<Enrollment> scopedEnrollments(AuthenticatedUser user, Integer athleteId) {
        if (user.role() == RoleName.GUARDIAN) {
            return guardianEnrollments(user).stream()
                    .filter(enrollment -> athleteId == null || athleteId.equals(enrollment.getAthlete().getId()))
                    .toList();
        }
        return athleteId == null ? null : enrollmentRepository.findByStatusAndAthleteId(EnrollmentStatus.ACTIVE, athleteId);
    }

    private List<Enrollment> guardianEnrollments(AuthenticatedUser user) {
        return enrollmentRepository.findByStatusAndAthleteGuardianId(EnrollmentStatus.ACTIVE,
                athleteAccessPolicy.ownGuardianId(user));
    }

    /**
     * @return los deportistas de esas matrículas que asisten a la sesión: mismo grupo y matrícula iniciada
     *         en el mes de la sesión o antes
     */
    private static List<SessionAthlete> attending(List<Enrollment> enrollments, TrainingSession session) {
        YearMonth month = YearMonth.from(session.getSessionDate());
        return enrollments.stream()
                .filter(enrollment -> enrollment.getGroup().getId().equals(session.getGroup().getId()))
                .filter(enrollment -> !enrollment.getPeriod().isAfter(month))
                .map(enrollment -> new SessionAthlete(enrollment.getAthlete().getId(),
                        enrollment.getAthlete().getFirstName()))
                .distinct()
                .sorted(ATHLETE_ORDER)
                .toList();
    }
}
