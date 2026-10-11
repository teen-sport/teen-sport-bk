package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales del login (esquema {@code LoginRequest}).
 *
 * @param email    correo de la cuenta
 * @param password contraseña en claro
 */
public record LoginRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = PasswordPolicy.MAX_LENGTH) String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
