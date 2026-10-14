package com.uniquindio.teen_sport.dto;

import jakarta.validation.Valid;

/**
 * Confirmación de una matrícula (esquema {@code ConfirmEnrollmentRequest}). El cuerpo es opcional.
 *
 * @param groupId otro grupo de la misma disciplina, opcional
 * @param payment pago que se registra en la misma operación, opcional; sin él la matrícula queda
 *                {@code OVERDUE}
 */
public record ConfirmEnrollmentRequest(Integer groupId, @Valid RegisterPaymentRequest payment) {
}
