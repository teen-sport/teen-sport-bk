package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo obligatorio (esquema {@code ReasonRequiredRequest}).
 *
 * @param reason motivo, de 1 a 500 caracteres
 */
public record ReasonRequiredRequest(@NotBlank @Size(max = 500) String reason) {
}
