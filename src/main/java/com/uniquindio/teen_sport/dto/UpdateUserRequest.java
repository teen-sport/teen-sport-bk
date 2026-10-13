package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edición de una cuenta por un administrador (esquema {@code UpdateUserRequest}).
 *
 * <p>El rol, el documento y el estado no se cambian aquí. El correo es opcional: si no se envía,
 * se conserva. Los demás datos siguen las reglas de {@link EditableProfile}.</p>
 *
 * @param firstNames        nombres
 * @param lastNames         apellidos
 * @param email             nuevo correo, opcional
 * @param phone             teléfono
 * @param homeAddress       dirección; solo acudientes
 * @param specialty         especialidad; solo entrenadores
 * @param yearsOfExperience años de experiencia; solo entrenadores
 */
public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String firstNames,
        @NotBlank @Size(max = 100) String lastNames,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 255) String homeAddress,
        @Size(max = 100) String specialty,
        @Min(0) Integer yearsOfExperience) implements EditableProfile {
}
