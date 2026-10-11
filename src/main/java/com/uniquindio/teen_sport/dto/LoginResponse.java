package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.RoleName;

/**
 * Resultado del login (esquema {@code LoginResponse}).
 *
 * @param accessToken      JWT firmado
 * @param tokenType        siempre {@code Bearer}
 * @param expiresInSeconds vigencia del token
 * @param user             datos mínimos del usuario autenticado
 */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, LoggedUser user) {

    /**
     * @param id         id de la cuenta
     * @param firstNames nombres
     * @param lastNames  apellidos
     * @param role       rol
     * @param guardianId id del perfil de acudiente, o {@code null}
     * @param coachId    id del perfil de entrenador, o {@code null}
     */
    public record LoggedUser(Integer id, String firstNames, String lastNames, RoleName role, Integer guardianId,
                             Integer coachId) {
    }
}
