package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Edición del nombre y la capacidad de un grupo (esquema {@code UpdateGroupRequest}).
 *
 * @param name        nombre
 * @param maxCapacity capacidad máxima, de 1 a 15 (fuera de rango: 422 {@code INVALID_MAX_CAPACITY})
 */
public record UpdateGroupRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Integer maxCapacity) {
}
