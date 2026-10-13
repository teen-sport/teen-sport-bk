package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.EntityStatus;

/**
 * Entrenador en el listado para asignar grupos (esquema {@code CoachListItem}).
 *
 * @param id                id del perfil de entrenador (el que usan H8 y H11)
 * @param userId            id de la cuenta
 * @param fullName          nombres y apellidos
 * @param specialty         especialidad
 * @param yearsOfExperience años de experiencia
 * @param status            estado de la cuenta
 */
public record CoachListItem(Integer id, Integer userId, String fullName, String specialty,
                            Integer yearsOfExperience, EntityStatus status) {
}
