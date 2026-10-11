package com.uniquindio.teen_sport.exception;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo uniforme de las respuestas de error (esquema {@code Error} del contrato).
 *
 * @param timestamp momento del error en UTC
 * @param status    código HTTP de la respuesta
 * @param code      código estable que el cliente debe evaluar
 * @param message   mensaje de presentación en español
 * @param details   campos rechazados; vacío cuando el error no es de validación
 * @param path      ruta de la solicitud que produjo el error
 */
public record ApiError(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        List<ApiErrorDetail> details,
        String path) {
}
