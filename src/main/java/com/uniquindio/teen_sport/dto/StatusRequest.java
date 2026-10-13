package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.EntityStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio de estado (esquema {@code StatusRequest}).
 *
 * @param status nuevo estado
 */
public record StatusRequest(@NotNull EntityStatus status) {
}
