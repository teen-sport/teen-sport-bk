package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Edición de un deportista (esquema {@code UpdateAthleteRequest}). El acudiente responsable no
 * cambia.
 *
 * @param firstNames nombres
 * @param lastNames  apellidos
 * @param idType     tipo de documento
 * @param idNumber   número de documento
 * @param birthDate  fecha de nacimiento
 * @param gender     género, opcional
 */
public record UpdateAthleteRequest(
        @NotBlank @Size(max = 100) String firstNames,
        @NotBlank @Size(max = 100) String lastNames,
        @NotBlank @Size(max = 10) String idType,
        @NotBlank @Size(max = 50) String idNumber,
        @NotNull LocalDate birthDate,
        @Size(max = 20) String gender) implements AthleteData {
}
