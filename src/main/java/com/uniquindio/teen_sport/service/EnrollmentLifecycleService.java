package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.ConfirmEnrollmentRequest;
import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.SecurityMessages;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.EnrollmentStatePolicy;
import com.uniquindio.teen_sport.validation.GroupEnrollmentPolicy;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revisión y ciclo de la matrícula: confirmar (E12), rechazar (E13), cancelar (E14) y cambiar de
 * grupo (E15).
 *
 * <p>Transiciones:</p>
 * <ul>
 *   <li>{@code PENDING_REVIEW} → {@code ACTIVE} (E12) o {@code REJECTED} (E13).</li>
 *   <li>{@code PENDING_REVIEW} o {@code ACTIVE} → {@code CANCELLED} (E14). Un acudiente solo cancela
 *       matrículas que aún no están {@code ACTIVE}.</li>
 *   <li>{@code REJECTED} y {@code CANCELLED} son finales; rechazarlas o cancelarlas libera el cupo,
 *       porque dejan de contar como vigentes.</li>
 *   <li>Cambiar de grupo (E12 con {@code groupId}, E15) exige un grupo activo de la misma disciplina
 *       con cupo, y conserva el mes de inicio. Las sesiones y asistencias pasadas no se tocan.</li>
 * </ul>
 *
 * <p>Concurrencia: cada operación bloquea primero la matrícula y, si cambia de grupo, después el grupo
 * de destino antes de contar sus cupos. Solo se bloquea el grupo que recibe la matrícula: el que la
 * pierde solo libera un cupo.</p>
 *
 * <p>E12 puede registrar un pago en la misma transacción, con las mismas reglas que E17
 * ({@link PaymentRecorder}). Pendientes: los correos {@code ENROLLMENT_CONFIRMED} y
 * {@code ENROLLMENT_REJECTED} (Fase 7).</p>
 */
@Service
public class EnrollmentLifecycleService {

    static final String NOT_FOUND_MESSAGE = "La matrícula no existe.";
    static final String GROUP_NOT_FOUND_MESSAGE = "El grupo no existe.";

    private final EnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;
    private final AthleteAccessPolicy accessPolicy;
    private final SpotCounter spotCounter;
    private final EnrollmentQueryService queryService;
    private final AcademyCalendar calendar;
    private final PaymentRecorder paymentRecorder;

    /**
     * @param enrollmentRepository matrículas
     * @param groupRepository      grupos
     * @param accessPolicy         regla de propiedad de los deportistas
     * @param spotCounter          cálculo de cupos
     * @param queryService         detalle de la matrícula en el formato del contrato
     * @param calendar             reloj de la academia
     * @param paymentRecorder      registro del pago opcional de E12
     */
    public EnrollmentLifecycleService(EnrollmentRepository enrollmentRepository, GroupRepository groupRepository,
                                      AthleteAccessPolicy accessPolicy, SpotCounter spotCounter,
                                      EnrollmentQueryService queryService, AcademyCalendar calendar,
                                      PaymentRecorder paymentRecorder) {
        this.enrollmentRepository = enrollmentRepository;
        this.groupRepository = groupRepository;
        this.accessPolicy = accessPolicy;
        this.spotCounter = spotCounter;
        this.queryService = queryService;
        this.calendar = calendar;
        this.paymentRecorder = paymentRecorder;
    }

    /**
     * E12: confirma una matrícula pendiente, opcionalmente en otro grupo de la misma disciplina y con
     * un pago. Sin pago, la matrícula sigue {@code OVERDUE}. Si el pago no cumple sus reglas, no se
     * confirma nada.
     *
     * @param user         administrador autenticado
     * @param enrollmentId id de la matrícula
     * @param request      grupo de destino y pago, ambos opcionales; puede ser {@code null}
     * @return la matrícula confirmada
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code INVALID_STATE}, {@code NO_AVAILABLE_SPOTS} o
     *                           {@code PAYMENT_ALREADY_REGISTERED}; 422 {@code GROUP_INACTIVE},
     *                           {@code GROUP_OTHER_DISCIPLINE}, {@code INSUFFICIENT_AMOUNT} o {@code FUTURE_DATE}
     */
    @Transactional
    public EnrollmentDetail confirm(AuthenticatedUser user, Integer enrollmentId, ConfirmEnrollmentRequest request) {
        Enrollment enrollment = findLocked(enrollmentId);
        EnrollmentStatePolicy.requirePending(enrollment);
        Integer groupId = request == null ? null : request.groupId();
        if (groupId == null || groupId.equals(enrollment.getGroup().getId())) {
            GroupEnrollmentPolicy.requireOpen(enrollment.getGroup());
        } else {
            moveTo(enrollment, groupId);
        }
        if (request != null && request.payment() != null) {
            paymentRecorder.record(enrollment, user.userId(), request.payment());
        }
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setConfirmedAt(calendar.now());
        return queryService.detail(enrollment);
    }

    /**
     * E13: rechaza una matrícula pendiente y libera su cupo.
     *
     * @param enrollmentId id de la matrícula
     * @param reason       motivo, que se guarda en las notas de revisión
     * @return la matrícula rechazada
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code INVALID_STATE}
     */
    @Transactional
    public EnrollmentDetail reject(Integer enrollmentId, String reason) {
        Enrollment enrollment = findLocked(enrollmentId);
        EnrollmentStatePolicy.requirePending(enrollment);
        enrollment.setStatus(EnrollmentStatus.REJECTED);
        enrollment.setReviewNotes(reason.trim());
        return queryService.detail(enrollment);
    }

    /**
     * E14: cancela una matrícula vigente y libera su cupo. El motivo, si se envía, se guarda en las
     * notas de revisión.
     *
     * @param user         identidad autenticada
     * @param enrollmentId id de la matrícula
     * @param reason       motivo opcional
     * @return la matrícula cancelada
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED} si es ajena o si un
     *                           acudiente intenta cancelar una matrícula {@code ACTIVE}; 409 {@code INVALID_STATE}
     */
    @Transactional
    public EnrollmentDetail cancel(AuthenticatedUser user, Integer enrollmentId, String reason) {
        Enrollment enrollment = findLocked(enrollmentId);
        accessPolicy.requireAccess(user, enrollment.getAthlete());
        EnrollmentStatePolicy.requireInForce(enrollment);
        if (user.role() != RoleName.ADMIN && enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, SecurityMessages.ACCESS_DENIED);
        }
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        String note = TextNormalizer.trimToNull(reason);
        if (note != null) {
            enrollment.setReviewNotes(note);
        }
        return queryService.detail(enrollment);
    }

    /**
     * E15: mueve una matrícula vigente a otro grupo de la misma disciplina, conservando su mes de
     * inicio. Indicar el grupo actual no cambia nada.
     *
     * @param enrollmentId id de la matrícula
     * @param groupId      grupo de destino
     * @return la matrícula actualizada
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code INVALID_STATE} o {@code NO_AVAILABLE_SPOTS};
     *                           422 {@code GROUP_INACTIVE} o {@code GROUP_OTHER_DISCIPLINE}
     */
    @Transactional
    public EnrollmentDetail changeGroup(Integer enrollmentId, Integer groupId) {
        Enrollment enrollment = findLocked(enrollmentId);
        EnrollmentStatePolicy.requireInForce(enrollment);
        if (!groupId.equals(enrollment.getGroup().getId())) {
            moveTo(enrollment, groupId);
        }
        return queryService.detail(enrollment);
    }

    private void moveTo(Enrollment enrollment, Integer groupId) {
        Group target = groupRepository.findLockedById(groupId)
                .orElseThrow(() -> notFound(GROUP_NOT_FOUND_MESSAGE));
        GroupEnrollmentPolicy.requireSameDiscipline(enrollment.getGroup(), target);
        GroupEnrollmentPolicy.requireOpen(target);
        spotCounter.requireFreeSpot(target);
        enrollment.setGroup(target);
    }

    private Enrollment findLocked(Integer enrollmentId) {
        return enrollmentRepository.findLockedById(enrollmentId).orElseThrow(() -> notFound(NOT_FOUND_MESSAGE));
    }

    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
