package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.AthleteAttendanceReport;
import com.uniquindio.teen_sport.dto.AttendanceReport;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.SecurityMessages;
import com.uniquindio.teen_sport.repository.AthleteRepository;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupMarkCount;
import com.uniquindio.teen_sport.repository.GroupSessionCount;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.DateRangePolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores de asistencia (R2) y asistencia de un deportista (R3), con los KPI del Proceso 2:
 * sesiones programadas frente a realizadas y porcentaje de asistencia por grupo y por deportista.
 *
 * <p>Reglas (decisión del 2026-11-10):</p>
 * <ul>
 *   <li>R2 sin fechas abarca del primer día del mes de {@code dateTo} (hoy por defecto) hasta
 *       {@code dateTo}. Solo cuentan las sesiones con fecha hasta hoy, como fija el contrato.</li>
 *   <li>R3 sin fechas abarca todo el historial del deportista.</li>
 *   <li>Con ambos extremos, el rango no puede superar {@value DateRangePolicy#MAX_REPORT_RANGE_DAYS}
 *       días.</li>
 *   <li>El entrenador solo ve sus grupos (los que tiene asignados hoy, como en A9). En R3 solo puede
 *       consultar a un deportista con matrícula vigente en uno de sus grupos y solo ve las marcas de
 *       sus grupos.</li>
 * </ul>
 */
@Service
public class AttendanceReportService {

    static final String ATHLETE_NOT_FOUND_MESSAGE = "El deportista no existe.";

    private final TrainingSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AthleteRepository athleteRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupAccessPolicy groupAccessPolicy;
    private final AthleteAccessPolicy athleteAccessPolicy;
    private final AcademyCalendar calendar;

    /**
     * @param sessionRepository    sesiones
     * @param attendanceRepository marcas de asistencia
     * @param athleteRepository    deportistas
     * @param enrollmentRepository matrículas
     * @param groupAccessPolicy    alcance de los entrenadores
     * @param athleteAccessPolicy  propiedad de los deportistas
     * @param calendar             fecha local de la academia
     */
    public AttendanceReportService(TrainingSessionRepository sessionRepository,
                                   AttendanceRecordRepository attendanceRepository,
                                   AthleteRepository athleteRepository, EnrollmentRepository enrollmentRepository,
                                   GroupAccessPolicy groupAccessPolicy, AthleteAccessPolicy athleteAccessPolicy,
                                   AcademyCalendar calendar) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.athleteRepository = athleteRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.groupAccessPolicy = groupAccessPolicy;
        this.athleteAccessPolicy = athleteAccessPolicy;
        this.calendar = calendar;
    }

    /**
     * R2: indicadores de asistencia por grupo. Un filtro de grupo o disciplina ajeno al entrenador no
     * devuelve datos, como en A9.
     *
     * @param user         identidad autenticada
     * @param dateFrom     primera fecha, o {@code null}
     * @param dateTo       última fecha, o {@code null}
     * @param groupId      grupo, o {@code null}
     * @param disciplineId disciplina, o {@code null}
     * @return el informe
     * @throws BusinessException 422 {@code INVALID_DATE_RANGE}; 403 {@code ACCESS_DENIED} si el rol no
     *                           puede consultar grupos
     */
    @Transactional(readOnly = true)
    public AttendanceReport attendanceReport(AuthenticatedUser user, LocalDate dateFrom, LocalDate dateTo,
                                             Integer groupId, Integer disciplineId) {
        LocalDate today = calendar.today();
        LocalDate to = dateTo != null ? dateTo : today;
        LocalDate from = dateFrom != null ? dateFrom : to.withDayOfMonth(1);
        DateRangePolicy.requireReportRange(from, to);
        Integer coachId = groupAccessPolicy.coachScope(user, null);
        LocalDate until = to.isAfter(today) ? today : to;

        List<GroupSessionCount> sessions =
                sessionRepository.countSessionsByGroup(from, until, groupId, disciplineId, coachId);
        Map<Integer, GroupMarkCount> marks = attendanceRepository
                .countMarksByGroup(from, until, groupId, disciplineId, coachId).stream()
                .collect(Collectors.toMap(GroupMarkCount::getGroupId, Function.identity()));

        List<AttendanceReport.GroupAttendance> byGroup = sessions.stream()
                .map(group -> groupAttendance(group, marks.get(group.getGroupId())))
                .toList();
        long scheduled = sessions.stream().mapToLong(GroupSessionCount::getScheduled).sum();
        long completed = sessions.stream().mapToLong(GroupSessionCount::getCompleted).sum();
        long present = marks.values().stream().mapToLong(GroupMarkCount::getPresent).sum();
        long absent = marks.values().stream().mapToLong(GroupMarkCount::getAbsent).sum();
        return new AttendanceReport(scheduled, completed, Percentages.of(completed, scheduled),
                Percentages.ofOrNull(present, present + absent), byGroup);
    }

    /**
     * R3: asistencia de un deportista por grupo.
     *
     * @param user      identidad autenticada
     * @param athleteId id del deportista
     * @param dateFrom  primera fecha, o {@code null}
     * @param dateTo    última fecha, o {@code null}
     * @return el informe
     * @throws BusinessException 422 {@code INVALID_DATE_RANGE}; 404 {@code NOT_FOUND}; 403
     *                           {@code ACCESS_DENIED} si el deportista no es del acudiente o no está en
     *                           un grupo del entrenador
     */
    @Transactional(readOnly = true)
    public AthleteAttendanceReport athleteReport(AuthenticatedUser user, Integer athleteId, LocalDate dateFrom,
                                                 LocalDate dateTo) {
        DateRangePolicy.requireReportRange(dateFrom, dateTo);
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        ATHLETE_NOT_FOUND_MESSAGE));
        Integer coachId = coachScopeFor(user, athlete);

        List<AthleteAttendanceReport.GroupMarks> byGroup = attendanceRepository
                .countAthleteMarksByGroup(athleteId, dateFrom, dateTo, coachId).stream()
                .map(group -> new AthleteAttendanceReport.GroupMarks(group.getGroupId(), group.getGroupName(),
                        group.getPresent(), group.getAbsent(),
                        Percentages.ofOrNull(group.getPresent(), group.getPresent() + group.getAbsent())))
                .toList();
        long present = byGroup.stream().mapToLong(AthleteAttendanceReport.GroupMarks::present).sum();
        long absent = byGroup.stream().mapToLong(AthleteAttendanceReport.GroupMarks::absent).sum();
        return new AthleteAttendanceReport(athleteId, present + absent, present, absent,
                Percentages.ofOrNull(present, present + absent), byGroup);
    }

    /**
     * @return el entrenador al que se limitan las marcas, o {@code null} para no limitarlas
     */
    private Integer coachScopeFor(AuthenticatedUser user, Athlete athlete) {
        if (user.role() == RoleName.ADMIN) {
            return null;
        }
        if (user.role() == RoleName.COACH) {
            Integer coachId = groupAccessPolicy.coachScope(user, null);
            if (!enrollmentRepository.existsByAthleteIdAndGroupCoachIdAndStatusIn(athlete.getId(), coachId,
                    EnrollmentStatus.IN_FORCE)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
                        SecurityMessages.ACCESS_DENIED);
            }
            return coachId;
        }
        athleteAccessPolicy.requireAccess(user, athlete);
        return null;
    }

    private static AttendanceReport.GroupAttendance groupAttendance(GroupSessionCount sessions, GroupMarkCount marks) {
        Double rate = marks == null ? null
                : Percentages.ofOrNull(marks.getPresent(), marks.getPresent() + marks.getAbsent());
        return new AttendanceReport.GroupAttendance(sessions.getGroupId(), sessions.getGroupName(),
                sessions.getDiscipline(), sessions.getScheduled(), sessions.getCompleted(), rate);
    }
}
