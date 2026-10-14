package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.dto.PaymentReportRequest;
import com.uniquindio.teen_sport.dto.PaymentResponse;
import com.uniquindio.teen_sport.dto.RegisterPaymentRequest;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.PaymentMapper;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.EnrollmentStatePolicy;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pagos presenciales de una matrícula: reporte del acudiente (E16), registro del administrador (E17)
 * y marca manual de mora (E18). No hay pasarela de pagos ni proceso automático (ADR-008).
 *
 * <p>Cada operación bloquea primero la matrícula, igual que su revisión y su ciclo (E12–E15). Así, dos
 * registros o reportes simultáneos del mismo mes se serializan y solo el primero queda guardado.</p>
 */
@Service
public class PaymentService {

    static final String NOT_FOUND_MESSAGE = "La matrícula no existe.";
    static final String USE_PAYMENT_REGISTRATION_MESSAGE =
            "El estado al día solo se alcanza registrando un pago.";

    private final EnrollmentRepository enrollmentRepository;
    private final AthleteAccessPolicy accessPolicy;
    private final PaymentRecorder paymentRecorder;
    private final EnrollmentQueryService queryService;
    private final AcademyCalendar calendar;

    /**
     * @param enrollmentRepository matrículas
     * @param accessPolicy         regla de propiedad de los deportistas
     * @param paymentRecorder      registro de pagos
     * @param queryService         detalle de la matrícula en el formato del contrato
     * @param calendar             reloj de la academia
     */
    public PaymentService(EnrollmentRepository enrollmentRepository, AthleteAccessPolicy accessPolicy,
                          PaymentRecorder paymentRecorder, EnrollmentQueryService queryService,
                          AcademyCalendar calendar) {
        this.enrollmentRepository = enrollmentRepository;
        this.accessPolicy = accessPolicy;
        this.paymentRecorder = paymentRecorder;
        this.queryService = queryService;
        this.calendar = calendar;
    }

    /**
     * E16: el acudiente reporta que pagó presencialmente. Solo marca la matrícula para que el
     * administrador la encuentre; no cambia su estado de pago.
     *
     * <p>La fecha indicada solo sirve para comprobar que ese mes no tenga ya un pago registrado; en
     * {@code payment_reported_at} se guarda el momento del reporte. Un reporte nuevo reemplaza la nota
     * del anterior.</p>
     *
     * @param user         acudiente autenticado
     * @param enrollmentId id de la matrícula
     * @param request      fecha y nota opcionales; puede ser {@code null}
     * @return la matrícula con el pago reportado
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED} si es ajena;
     *                           409 {@code INVALID_STATE} o {@code PAYMENT_ALREADY_REGISTERED}
     */
    @Transactional
    public EnrollmentDetail report(AuthenticatedUser user, Integer enrollmentId, PaymentReportRequest request) {
        Enrollment enrollment = findLocked(enrollmentId);
        accessPolicy.ownGuardianId(user);
        accessPolicy.requireAccess(user, enrollment.getAthlete());
        EnrollmentStatePolicy.requireInForce(enrollment);
        boolean hasDate = request != null && request.paymentDate() != null;
        paymentRecorder.requireMonthFree(enrollment,
                hasDate ? YearMonth.from(request.paymentDate()) : calendar.currentPeriod());
        enrollment.setPaymentReported(true);
        enrollment.setPaymentReportedAt(calendar.now());
        enrollment.setPaymentReportNote(request == null ? null : TextNormalizer.trimToNull(request.notes()));
        return queryService.detail(enrollment);
    }

    /**
     * E17: registra un pago presencial y deja la matrícula al día.
     *
     * @param user         administrador autenticado
     * @param enrollmentId id de la matrícula
     * @param request      datos del pago
     * @return el pago registrado
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code INVALID_STATE} o
     *                           {@code PAYMENT_ALREADY_REGISTERED}; 422 {@code INSUFFICIENT_AMOUNT} o {@code FUTURE_DATE}
     */
    @Transactional
    public PaymentResponse register(AuthenticatedUser user, Integer enrollmentId, RegisterPaymentRequest request) {
        Enrollment enrollment = findLocked(enrollmentId);
        EnrollmentStatePolicy.requireInForce(enrollment);
        return PaymentMapper.toResponse(paymentRecorder.record(enrollment, user.userId(), request));
    }

    /**
     * E18: marca manualmente una matrícula en mora. Se admite en cualquier estado de la matrícula y,
     * si ya estaba en mora, no cambia nada.
     *
     * @param enrollmentId  id de la matrícula
     * @param paymentStatus estado pedido; solo {@code OVERDUE}
     * @return la matrícula actualizada
     * @throws BusinessException 422 {@code USE_PAYMENT_REGISTRATION} si se pide {@code UP_TO_DATE};
     *                           404 {@code NOT_FOUND}
     */
    @Transactional
    public EnrollmentDetail markOverdue(Integer enrollmentId, PaymentStatus paymentStatus) {
        if (paymentStatus != PaymentStatus.OVERDUE) {
            throw new BusinessException(ErrorCode.USE_PAYMENT_REGISTRATION, HttpStatus.UNPROCESSABLE_CONTENT,
                    USE_PAYMENT_REGISTRATION_MESSAGE);
        }
        Enrollment enrollment = findLocked(enrollmentId);
        enrollment.setPaymentStatus(PaymentStatus.OVERDUE);
        return queryService.detail(enrollment);
    }

    private Enrollment findLocked(Integer enrollmentId) {
        return enrollmentRepository.findLockedById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
    }
}
