package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Restablecimiento de la contraseña con el token del correo (esquema {@code PasswordResetConfirmRequest}, U14).
 *
 * @param token       token recibido en el enlace del correo
 * @param newPassword nueva contraseña; debe cumplir {@link PasswordPolicy}
 */
public record PasswordResetConfirmRequest(
        @NotBlank @Size(max = MAX_TOKEN_LENGTH) String token,
        @NotNull @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String newPassword) {

    /** Límite de entrada; los tokens emitidos tienen 43 caracteres. */
    public static final int MAX_TOKEN_LENGTH = 100;

    @Override
    public String toString() {
        return "PasswordResetConfirmRequest[token=****, newPassword=****]";
    }
}
