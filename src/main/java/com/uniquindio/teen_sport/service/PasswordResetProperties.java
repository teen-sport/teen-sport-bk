package com.uniquindio.teen_sport.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Recuperación de contraseña ({@code app.password-reset.*}).
 *
 * @param url           página del frontend que recibe el token; el correo enlaza a {@code url?token=...}
 * @param tokenValidity vigencia de cada token (aprobada en 20 minutos)
 */
@ConfigurationProperties("app.password-reset")
public record PasswordResetProperties(String url, Duration tokenValidity) {

    /**
     * @throws IllegalArgumentException si falta la página o la vigencia no es positiva
     */
    public PasswordResetProperties {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("app.password-reset.url es obligatoria");
        }
        if (tokenValidity == null || tokenValidity.isNegative() || tokenValidity.isZero()) {
            throw new IllegalArgumentException("app.password-reset.token-validity debe ser positiva");
        }
    }
}
