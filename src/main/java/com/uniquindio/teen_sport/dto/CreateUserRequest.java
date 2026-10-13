package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creación de una cuenta de personal por un administrador (esquema {@code CreateUserRequest}).
 *
 * <p>Solo admite los roles {@code ADMIN} y {@code COACH}: los acudientes se registran por U2.
 * {@code specialty} y {@code yearsOfExperience} solo se usan para entrenadores. Los límites
 * siguen el contrato y, donde el contrato no fija uno, el tamaño de la columna en el MER.</p>
 *
 * @param role              rol de la cuenta
 * @param firstNames        nombres
 * @param lastNames         apellidos
 * @param idType            tipo de documento
 * @param idNumber          número de documento
 * @param email             correo
 * @param phone             teléfono
 * @param password          contraseña inicial en claro
 * @param specialty         especialidad; solo entrenadores
 * @param yearsOfExperience años de experiencia; solo entrenadores
 */
public record CreateUserRequest(
        @NotNull RoleName role,
        @NotBlank @Size(max = 100) String firstNames,
        @NotBlank @Size(max = 100) String lastNames,
        @NotBlank @Size(max = 10) String idType,
        @NotBlank @Size(max = 20) String idNumber,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String phone,
        @NotNull @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String password,
        @Size(max = 100) String specialty,
        @Min(0) Integer yearsOfExperience) {

    /**
     * @return {@code true} si el rol es de personal; el error se reporta en el campo {@code staffRole}
     */
    @AssertTrue(message = "el rol debe ser ADMIN o COACH")
    public boolean isStaffRole() {
        return role == null || role == RoleName.ADMIN || role == RoleName.COACH;
    }

    @Override
    public String toString() {
        return "CreateUserRequest[role=" + role + ", email=" + email + ", password=****]";
    }
}
