package com.uniquindio.teen_sport.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de los JWT ({@code app.security.jwt.*}).
 *
 * <p>Se valida al arrancar: HS256 exige un secreto de al menos 256 bits y la vigencia debe ser
 * positiva. Si no se cumple, la aplicación no inicia en lugar de emitir tokens débiles.</p>
 *
 * @param secret            secreto HMAC, recibido por la variable de entorno {@code JWT_SECRET}
 * @param expirationSeconds vigencia de cada token en segundos
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, long expirationSeconds) {

    /** Longitud mínima del secreto en bytes (256 bits para HS256). */
    public static final int MIN_SECRET_BYTES = 32;

    /**
     * @throws IllegalArgumentException si el secreto es demasiado corto o la vigencia no es positiva
     */
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.security.jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("app.security.jwt.expiration-seconds debe ser positivo");
        }
    }
}
