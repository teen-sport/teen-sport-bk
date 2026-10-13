package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Actualización del perfil propio (esquema {@code UpdateOwnProfileRequest}).
 *
 * <p>No incluye correo, documento ni rol: esos datos no se pueden cambiar desde el perfil propio
 * y, si el cliente los envía, se ignoran. Los campos de un rol distinto al del usuario también se
 * ignoran. Los límites de tamaño siguen las columnas del MER.</p>
 *
 * @param firstNames        nombres
 * @param lastNames         apellidos
 * @param phone             teléfono
 * @param homeAddress       dirección; solo acudientes
 * @param specialty         especialidad; solo entrenadores
 * @param yearsOfExperience años de experiencia; solo entrenadores
 */
public record UpdateOwnProfileRequest(
        @NotBlank @Size(max = 100) String firstNames,
        @NotBlank @Size(max = 100) String lastNames,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 255) String homeAddress,
        @Size(max = 100) String specialty,
        @Min(0) Integer yearsOfExperience) implements EditableProfile {
}
