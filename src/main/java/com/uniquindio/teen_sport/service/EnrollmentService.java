package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.CreateEnrollmentRequest;
import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.AthleteRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.AthleteAgePolicy;
import com.uniquindio.teen_sport.validation.PeriodPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitud de matrícula (E6).
 *
 * <p>Reglas, en este orden:</p>
 * <ul>
 *   <li>El deportista y el grupo deben existir.</li>
 *   <li>Un acudiente solo matricula a sus deportistas ({@link AthleteAccessPolicy}).</li>
 *   <li>El deportista debe tener de 12 a 17 años en la fecha de la solicitud.</li>
 *   <li>El periodo (mes de inicio) no puede ser anterior al mes actual.</li>
 *   <li>El grupo y su disciplina deben estar activos.</li>
 *   <li>Un deportista no puede tener dos matrículas vigentes en la misma disciplina.</li>
 *   <li>Debe quedar cupo ({@link SpotCounter}).</li>
 * </ul>
 *
 * <p>Concurrencia: se bloquean el deportista y después el grupo, siempre en ese orden, antes de
 * comprobar duplicados y cupos. Así dos solicitudes por el último cupo, o dos solicitudes del mismo
 * deportista en la misma disciplina, se serializan y solo una prospera. El grupo se lee con el bloqueo
 * para que su estado no cambie (H10) entre la comprobación y la reserva.</p>
 *
 * <p>Pendiente de la Fase 7: el correo {@code NO_SPOTS_AVAILABLE} cuando no hay cupo.</p>
 */
@Service
public class EnrollmentService {

    static final String ATHLETE_NOT_FOUND_MESSAGE = "El deportista no existe.";
    static final String GROUP_NOT_FOUND_MESSAGE = "El grupo no existe.";
    static final String GROUP_INACTIVE_MESSAGE = "El grupo no está disponible para matrículas.";
    static final String DUPLICATE_MESSAGE = "El deportista ya tiene una matrícula vigente en esta disciplina.";

    private final EnrollmentRepository enrollmentRepository;
    private final AthleteRepository athleteRepository;
    private final GroupRepository groupRepository;
    private final AthleteAccessPolicy accessPolicy;
    private final SpotCounter spotCounter;
    private final EnrollmentQueryService queryService;
    private final AcademyCalendar calendar;

    /**
     * @param enrollmentRepository matrículas
     * @param athleteRepository    deportistas
     * @param groupRepository      grupos
     * @param accessPolicy         regla de propiedad de los deportistas
     * @param spotCounter          cálculo de cupos
     * @param queryService         detalle de la matrícula en el formato del contrato
     * @param calendar             fecha de negocio y reloj de la academia
     */
    public EnrollmentService(EnrollmentRepository enrollmentRepository, AthleteRepository athleteRepository,
                             GroupRepository groupRepository, AthleteAccessPolicy accessPolicy,
                             SpotCounter spotCounter, EnrollmentQueryService queryService, AcademyCalendar calendar) {
        this.enrollmentRepository = enrollmentRepository;
        this.athleteRepository = athleteRepository;
        this.groupRepository = groupRepository;
        this.accessPolicy = accessPolicy;
        this.spotCounter = spotCounter;
        this.queryService = queryService;
        this.calendar = calendar;
    }

    /**
     * E6: solicita una matrícula, que queda {@code PENDING_REVIEW} con el pago {@code OVERDUE} y
     * reserva un cupo.
     *
     * @param user    identidad autenticada
     * @param request deportista, grupo y mes de inicio
     * @return la matrícula creada
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED};
     *                           422 {@code AGE_OUT_OF_RANGE}, {@code PERIOD_IN_PAST} o {@code GROUP_INACTIVE};
     *                           409 {@code DUPLICATE_ENROLLMENT} o {@code NO_AVAILABLE_SPOTS}
     */
    @Transactional
    public EnrollmentDetail create(AuthenticatedUser user, CreateEnrollmentRequest request) {
        Athlete athlete = athleteRepository.findLockedById(request.athleteId())
                .orElseThrow(() -> notFound(ATHLETE_NOT_FOUND_MESSAGE));
        accessPolicy.requireAccess(user, athlete);
        Group group = groupRepository.findLockedById(request.groupId())
                .orElseThrow(() -> notFound(GROUP_NOT_FOUND_MESSAGE));
        AthleteAgePolicy.requireEligible(athlete.getBirthDate(), calendar.today());
        PeriodPolicy.requireNotPast(request.period(), calendar.currentPeriod());
        requireOpenForEnrollment(group);
        if (enrollmentRepository.existsByAthleteIdAndGroupDisciplineIdAndStatusIn(athlete.getId(),
                group.getDiscipline().getId(), EnrollmentStatus.IN_FORCE)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENROLLMENT, HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }
        spotCounter.requireFreeSpot(group);
        Enrollment enrollment = enrollmentRepository.save(
                new Enrollment(athlete, group, request.period(), calendar.now()));
        return queryService.detail(enrollment);
    }

    private static void requireOpenForEnrollment(Group group) {
        if (group.getStatus() != EntityStatus.ACTIVE || group.getDiscipline().getStatus() != EntityStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.GROUP_INACTIVE, HttpStatus.UNPROCESSABLE_CONTENT,
                    GROUP_INACTIVE_MESSAGE);
        }
    }

    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
