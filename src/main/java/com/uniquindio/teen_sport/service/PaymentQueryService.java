package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.PaymentResponse;
import com.uniquindio.teen_sport.entity.PaymentHistory;
import com.uniquindio.teen_sport.exception.FieldValidationException;
import com.uniquindio.teen_sport.mapper.PaymentMapper;
import com.uniquindio.teen_sport.repository.PaymentFilter;
import com.uniquindio.teen_sport.repository.PaymentHistoryRepository;
import com.uniquindio.teen_sport.repository.PaymentSpecifications;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de pagos (E19). El administrador ve todos; un acudiente, solo los de sus deportistas
 * ({@link AthleteAccessPolicy}).
 */
@Service
public class PaymentQueryService {

    static final String RANGE_MESSAGE = "La fecha inicial no puede ser posterior a la final.";

    /** Orden del historial: el pago más reciente primero e id para desempatar. */
    static final Sort ORDER = Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"));

    private final PaymentHistoryRepository paymentRepository;
    private final AthleteAccessPolicy accessPolicy;

    /**
     * @param paymentRepository historial de pagos
     * @param accessPolicy      regla de propiedad de los deportistas
     */
    public PaymentQueryService(PaymentHistoryRepository paymentRepository, AthleteAccessPolicy accessPolicy) {
        this.paymentRepository = paymentRepository;
        this.accessPolicy = accessPolicy;
    }

    /**
     * @param user   identidad autenticada
     * @param filter filtros; su {@code guardianId} se ignora y lo fija el rol
     * @param page   índice de página, desde 0
     * @param size   tamaño de página
     * @return la página de pagos
     * @throws FieldValidationException 400 si {@code from} es posterior a {@code to}
     */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(AuthenticatedUser user, PaymentFilter filter, int page, int size) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new FieldValidationException("from", RANGE_MESSAGE);
        }
        PaymentFilter scoped = filter.withGuardian(accessPolicy.guardianScope(user, null));
        Page<PaymentHistory> payments = paymentRepository.findAll(PaymentSpecifications.payments(scoped),
                PageRequest.of(page, size, ORDER));
        return PageResponse.of(payments, payments.getContent().stream()
                .map(PaymentMapper::toResponse)
                .toList());
    }
}
