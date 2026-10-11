package com.uniquindio.teen_sport.exception;

/**
 * Detalle de un campo inválido dentro de una respuesta de error (esquema {@code ErrorDetail}).
 *
 * @param field   nombre del campo o parámetro rechazado
 * @param message motivo del rechazo, en español
 */
public record ApiErrorDetail(String field, String message) {
}
