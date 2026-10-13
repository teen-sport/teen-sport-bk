package com.uniquindio.teen_sport.dto;

/**
 * Referencia breve a una persona (esquema {@code PersonRef}).
 *
 * @param id       id del perfil referenciado (por ejemplo, el id de entrenador)
 * @param fullName nombres y apellidos
 */
public record PersonRef(Integer id, String fullName) {
}
