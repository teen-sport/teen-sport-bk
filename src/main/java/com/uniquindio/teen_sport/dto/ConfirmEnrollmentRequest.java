package com.uniquindio.teen_sport.dto;

/**
 * Confirmación de una matrícula (esquema {@code ConfirmEnrollmentRequest}). El cuerpo es opcional.
 *
 * <p>El campo {@code payment} del contrato (registrar el pago en la misma llamada) se incorpora con
 * los pagos en la Fase 5; mientras tanto no se lee y la matrícula queda {@code OVERDUE}.</p>
 *
 * @param groupId otro grupo de la misma disciplina, opcional
 */
public record ConfirmEnrollmentRequest(Integer groupId) {
}
