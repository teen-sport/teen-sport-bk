package com.uniquindio.teen_sport.dto;

/**
 * Acudiente en el listado para registrar deportistas en su nombre (esquema {@code GuardianListItem}).
 *
 * @param id       id del perfil de acudiente (el que usa E1)
 * @param userId   id de la cuenta
 * @param fullName nombres y apellidos
 * @param email    correo
 * @param phone    teléfono
 */
public record GuardianListItem(Integer id, Integer userId, String fullName, String email, String phone) {
}
