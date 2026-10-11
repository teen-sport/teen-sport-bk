package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro público de un acudiente (esquema {@code RegisterGuardianRequest}).
 *
 * <p>No incluye rol: el registro público siempre crea un {@code GUARDIAN}. Los límites siguen el
 * contrato y, donde el contrato no fija uno, el tamaño de la columna en el MER.</p>
 *
 * @param firstNames         nombres
 * @param lastNames          apellidos
 * @param idType             tipo de documento (por ejemplo, CC o CE)
 * @param idNumber           número de documento
 * @param email              correo
 * @param phone              teléfono
 * @param homeAddress        dirección, opcional
 * @param password           contraseña en claro
 * @param acceptedDataPolicy aceptación de la política de datos; debe ser {@code true}
 */
public record RegisterGuardianRequest(
        @NotBlank @Size(max = 100) String firstNames,
        @NotBlank @Size(max = 100) String lastNames,
        @NotBlank @Size(max = 10) String idType,
        @NotBlank @Size(max = 20) String idNumber,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 255) String homeAddress,
        @NotNull @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String password,
        @NotNull Boolean acceptedDataPolicy) {

    @Override
    public String toString() {
        return "RegisterGuardianRequest[email=" + email + ", password=****]";
    }
}
