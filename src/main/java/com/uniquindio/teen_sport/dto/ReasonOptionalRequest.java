package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.Size;

/**
 * Motivo opcional (esquema {@code ReasonOptionalRequest}).
 *
 * @param reason motivo, hasta 500 caracteres
 */
public record ReasonOptionalRequest(@Size(max = 500) String reason) {
}
