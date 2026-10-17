package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.RegisterPaymentRequest;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.PaymentHistory;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.PaymentHistoryRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Registro de un pago presencial, compartido por E17 y por la confirmación con pago (E12).
 *
 * <p>Reglas: el monto debe ser al menos la mensualidad vigente de la disciplina, la fecha no puede ser
 * futura en la zona de la academia y la matrícula admite como máximo un pago por mes calendario de
 * {@code paymentDate}. Al registrarlo, la matrícula pasa a {@code UP_TO_DATE} y deja de tener un pago
 * reportado pendiente; la fecha y la nota del último reporte se conservan.</p>
 *
 * <p>Quien llama debe tener bloqueada la matrícula y haber comprobado que está vigente: el bloqueo
 * serializa los registros y reportes de la misma matrícula, así que la comprobación del mes no puede
 * quedar desactualizada antes de guardar.</p>
 */
@Component
public class PaymentRecorder {

    static final String INSUFFICIENT_AMOUNT_MESSAGE = "El monto debe ser al menos la mensualidad de la disciplina.";
    static final String FUTURE_DATE_MESSAGE = "La fecha de pago no puede ser futura.";
    static final String ALREADY_REGISTERED_MESSAGE = "La matrícula ya tiene un pago registrado en ese mes.";

    private final PaymentHistoryRepository paymentRepository;
    private final UserRepository userRepository;
    private final AcademyCalendar calendar;

    /**
     * @param paymentRepository historial de pagos
     * @param userRepository    cuentas, para referenciar al administrador
     * @param calendar          reloj de la academia
     */
    public PaymentRecorder(PaymentHistoryRepository paymentRepository, UserRepository userRepository,
                           AcademyCalendar calendar) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.calendar = calendar;
    }

    /**
     * @param enrollment  matrícula vigente y bloqueada
     * @param adminUserId cuenta del administrador que registra el pago
     * @param request     datos del pago
     * @return el pago guardado
     * @throws BusinessException 422 {@code INSUFFICIENT_AMOUNT} o {@code FUTURE_DATE};
     *                           409 {@code PAYMENT_ALREADY_REGISTERED}
     */
    PaymentHistory record(Enrollment enrollment, Integer adminUserId, RegisterPaymentRequest request) {
        if (request.amountPaid().compareTo(enrollment.getGroup().getDiscipline().getMonthlyFee()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_AMOUNT, HttpStatus.UNPROCESSABLE_CONTENT,
                    INSUFFICIENT_AMOUNT_MESSAGE);
        }
        if (request.paymentDate().isAfter(calendar.today())) {
            throw new BusinessException(ErrorCode.FUTURE_DATE, HttpStatus.UNPROCESSABLE_CONTENT, FUTURE_DATE_MESSAGE);
        }
        requireMonthFree(enrollment, YearMonth.from(request.paymentDate()));
        PaymentHistory payment = new PaymentHistory(enrollment, userRepository.getReferenceById(adminUserId),
                request.amountPaid(), request.paymentDate(), calendar.now());
        payment.setReceiptReference(TextNormalizer.trimToNull(request.receiptReference()));
        payment.setNotes(TextNormalizer.trimToNull(request.notes()));
        enrollment.setPaymentStatus(PaymentStatus.UP_TO_DATE);
        enrollment.setPaymentReported(false);
        return paymentRepository.save(payment);
    }

    /**
     * @param enrollment matrícula bloqueada
     * @param month      mes calendario
     * @throws BusinessException 409 {@code PAYMENT_ALREADY_REGISTERED} si ya hay un pago fechado en ese mes
     */
    void requireMonthFree(Enrollment enrollment, YearMonth month) {
        if (paymentRepository.existsByEnrollmentIdAndPaymentDateBetween(enrollment.getId(), month.atDay(1),
                month.atEndOfMonth())) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_REGISTERED, HttpStatus.CONFLICT,
                    ALREADY_REGISTERED_MESSAGE);
        }
    }
}
