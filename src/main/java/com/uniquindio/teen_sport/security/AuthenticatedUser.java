package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.entity.RoleName;

/**
 * Identidad que viaja en el JWT: usuario, rol y, según el rol, su perfil de acudiente o entrenador.
 *
 * @param userId     id de la cuenta (claim {@code sub})
 * @param role       rol (claim {@code role})
 * @param guardianId id del perfil de acudiente; solo para {@link RoleName#GUARDIAN}
 * @param coachId    id del perfil de entrenador; solo para {@link RoleName#COACH}
 */
public record AuthenticatedUser(Integer userId, RoleName role, Integer guardianId, Integer coachId) {
}
