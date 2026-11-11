package com.uniquindio.teen_sport.dto;

/**
 * Contacto del acudiente de un deportista para emergencias (esquema {@code GuardianContact}). Solo se
 * expone en la lista de asistencia (A7, A8), al entrenador del grupo y al administrador.
 *
 * @param firstNames nombres
 * @param lastNames  apellidos
 * @param phone      teléfono, o {@code null} si la cuenta no lo tiene
 */
public record GuardianContact(String firstNames, String lastNames, String phone) {
}
