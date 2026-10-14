package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.PaymentReportRequest;
import com.uniquindio.teen_sport.dto.PaymentResponse;
import com.uniquindio.teen_sport.dto.PaymentStatusRequest;
import com.uniquindio.teen_sport.dto.RegisterPaymentRequest;
import com.uniquindio.teen_sport.repository.PaymentFilter;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.PaymentQueryService;
import com.uniquindio.teen_sport.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pagos presenciales (E16–E19). Solo el acudiente reporta un pago; solo el administrador registra
 * pagos o marca la mora. La propiedad de cada matrícula se comprueba en el servicio con la identidad
 * del token.
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'GUARDIAN')")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentQueryService queryService;
    private final CurrentUserService currentUserService;

    /**
     * @param paymentService     reporte, registro y estado de los pagos
     * @param queryService       historial de pagos
     * @param currentUserService identidad del usuario autenticado
     */
    public PaymentController(PaymentService paymentService, PaymentQueryService queryService,
                             CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.queryService = queryService;
        this.currentUserService = currentUserService;
    }

    /**
     * E16: el acudiente reporta que pagó presencialmente.
     *
     * @param id      id de la matrícula
     * @param request fecha y nota opcionales; el cuerpo puede omitirse
     * @return 200 con la matrícula
     */
    @PostMapping("/enrollments/{id}/payment-report")
    @PreAuthorize("hasRole('GUARDIAN')")
    public EnrollmentDetail reportPayment(@PathVariable Integer id,
                                          @Valid @RequestBody(required = false) PaymentReportRequest request) {
        return paymentService.report(currentUserService.current(), id, request);
    }

    /**
     * E17: registra un pago presencial.
     *
     * @param id      id de la matrícula
     * @param request datos del pago
     * @return 201 con el pago
     */
    @PostMapping("/enrollments/{id}/payments")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse registerPayment(@PathVariable Integer id,
                                           @Valid @RequestBody RegisterPaymentRequest request) {
        return paymentService.register(currentUserService.current(), id, request);
    }

    /**
     * E18: marca manualmente una matrícula en mora.
     *
     * @param id      id de la matrícula
     * @param request estado de pago; solo {@code OVERDUE}
     * @return 200 con la matrícula
     */
    @PatchMapping("/enrollments/{id}/payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentDetail updatePaymentStatus(@PathVariable Integer id,
                                                @Valid @RequestBody PaymentStatusRequest request) {
        return paymentService.markOverdue(id, request.paymentStatus());
    }

    /**
     * E19: historial paginado; un acudiente solo recibe los pagos de sus deportistas.
     *
     * @param page         índice de página, desde 0
     * @param size         tamaño de página, de 1 a 100
     * @param enrollmentId filtro por matrícula
     * @param athleteId    filtro por deportista
     * @param from         fecha de pago mínima, incluida
     * @param to           fecha de pago máxima, incluida
     * @return 200 con la página
     */
    @GetMapping("/payments")
    public PageResponse<PaymentResponse> listPayments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Integer enrollmentId,
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queryService.list(currentUserService.current(),
                new PaymentFilter(enrollmentId, athleteId, from, to, null), page, size);
    }
}
