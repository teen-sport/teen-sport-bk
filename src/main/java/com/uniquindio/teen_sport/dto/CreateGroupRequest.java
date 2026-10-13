package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creación de un grupo (esquema {@code CreateGroupRequest}).
 *
 * <p>El rango de {@code maxCapacity} (1 a 15) es una regla de negocio y se responde con 422
 * {@code INVALID_MAX_CAPACITY}; aquí solo se exige que venga.</p>
 *
 * @param disciplineId disciplina, activa
 * @param coachId      entrenador, activo
 * @param name         nombre
 * @param maxCapacity  capacidad máxima
 */
public record CreateGroupRequest(
        @NotNull Integer disciplineId,
        @NotNull Integer coachId,
        @NotBlank @Size(max = 100) String name,
        @NotNull Integer maxCapacity) {
}
