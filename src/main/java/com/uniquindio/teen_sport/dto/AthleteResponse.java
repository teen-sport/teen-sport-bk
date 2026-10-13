package com.uniquindio.teen_sport.dto;

import java.time.LocalDate;

/**
 * Deportista en las respuestas de la API (esquema {@code Athlete}).
 *
 * @param id                  id
 * @param guardianId          acudiente responsable
 * @param firstNames          nombres
 * @param lastNames           apellidos
 * @param idType              tipo de documento
 * @param idNumber            número de documento
 * @param birthDate           fecha de nacimiento
 * @param age                 edad en la fecha de la respuesta, según la zona de la academia
 * @param gender              género
 * @param hasActiveEnrollment si tiene una matrícula vigente
 */
public record AthleteResponse(Integer id, Integer guardianId, String firstNames, String lastNames, String idType,
                              String idNumber, LocalDate birthDate, int age, String gender,
                              boolean hasActiveEnrollment) {
}
