package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Registro de un deportista (esquema {@code CreateAthleteRequest}).
 *
 * <p>{@code guardianId} solo lo usa un administrador; para un acudiente se ignora y se toma del
 * token. Los límites de tamaño siguen las columnas del MER.</p>
 *
 * @param guardianId acudiente responsable; obligatorio para un administrador
 * @param firstNames nombres
 * @param lastNames  apellidos
 * @param idType     tipo de documento (por ejemplo, TI)
 * @param idNumber   número de documento
 * @param birthDate  fecha de nacimiento
 * @param gender     género, opcional
 */
public record CreateAthleteRequest(
        Integer guardianId,
        @NotBlank @Size(max = 100) String firstNames,
        @NotBlank @Size(max = 100) String lastNames,
        @NotBlank @Size(max = 10) String idType,
        @NotBlank @Size(max = 50) String idNumber,
        @NotNull LocalDate birthDate,
        @Size(max = 20) String gender) implements AthleteData {
}
