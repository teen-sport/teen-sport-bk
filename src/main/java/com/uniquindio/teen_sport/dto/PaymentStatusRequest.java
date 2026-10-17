package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio manual del estado de pago (esquema {@code PaymentStatusRequest}). Solo se acepta
 * {@code OVERDUE}; el servicio rechaza {@code UP_TO_DATE} con un error que indica registrar un pago.
 *
 * @param paymentStatus nuevo estado de pago
 */
public record PaymentStatusRequest(@NotNull PaymentStatus paymentStatus) {
}
