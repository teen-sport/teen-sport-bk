package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.RoleName;

/**
 * Perfil de un usuario (esquema {@code UserProfile}). Nunca incluye la contraseña ni su hash.
 *
 * @param id                 id de la cuenta
 * @param idType             tipo de documento
 * @param idNumber           número de documento
 * @param firstNames         nombres
 * @param lastNames          apellidos
 * @param email              correo
 * @param phone              teléfono
 * @param role               rol
 * @param status             estado
 * @param acceptedDataPolicy aceptación de la política de datos
 * @param guardian           datos de acudiente; solo para {@code GUARDIAN}
 * @param coach              datos de entrenador; solo para {@code COACH}
 */
public record UserProfile(
        Integer id,
        String idType,
        String idNumber,
        String firstNames,
        String lastNames,
        String email,
        String phone,
        RoleName role,
        EntityStatus status,
        boolean acceptedDataPolicy,
        GuardianData guardian,
        CoachData coach) {

    /**
     * @param id          id del perfil de acudiente
     * @param homeAddress dirección
     */
    public record GuardianData(Integer id, String homeAddress) {
    }

    /**
     * @param id                id del perfil de entrenador
     * @param specialty         especialidad
     * @param yearsOfExperience años de experiencia
     */
    public record CoachData(Integer id, String specialty, Integer yearsOfExperience) {
    }
}
