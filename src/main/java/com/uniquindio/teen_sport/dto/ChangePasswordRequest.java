package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambio de la contraseña propia (esquema {@code ChangePasswordRequest}).
 *
 * @param currentPassword contraseña actual, para confirmar la identidad
 * @param newPassword     nueva contraseña; debe cumplir {@link PasswordPolicy}
 */
public record ChangePasswordRequest(
        @NotBlank @Size(max = PasswordPolicy.MAX_LENGTH) String currentPassword,
        @NotNull @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=****, newPassword=****]";
    }
}
