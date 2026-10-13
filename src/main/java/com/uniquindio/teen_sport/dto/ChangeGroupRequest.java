package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de grupo de una matrícula (esquema {@code ChangeGroupRequest}).
 *
 * @param groupId nuevo grupo, de la misma disciplina
 */
public record ChangeGroupRequest(@NotNull Integer groupId) {
}
