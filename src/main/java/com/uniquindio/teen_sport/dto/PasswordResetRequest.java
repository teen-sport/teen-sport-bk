package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de recuperación de contraseña (esquema {@code PasswordResetRequest}, U13).
 *
 * @param email correo de la cuenta
 */
public record PasswordResetRequest(@NotBlank @Email @Size(max = 150) String email) {
}
