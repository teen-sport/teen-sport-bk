package com.uniquindio.teen_sport.config;

import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del administrador inicial ({@code app.bootstrap.admin.*}), recibidos por variables de entorno.
 *
 * <p>El administrador solo se crea cuando se definen correo, contraseña y número de documento.
 * Definir solo una parte es un error de configuración.</p>
 *
 * @param email      correo ({@code ADMIN_EMAIL})
 * @param password   contraseña en claro ({@code ADMIN_PASSWORD}); debe cumplir la política
 * @param idNumber   número de documento ({@code ADMIN_ID_NUMBER})
 * @param idType     tipo de documento ({@code ADMIN_ID_TYPE})
 * @param firstNames nombres ({@code ADMIN_FIRST_NAMES})
 * @param lastNames  apellidos ({@code ADMIN_LAST_NAMES})
 */
@ConfigurationProperties("app.bootstrap.admin")
public record AdminBootstrapProperties(String email, String password, String idNumber, String idType,
                                       String firstNames, String lastNames) {

    /**
     * @return {@code true} si se definieron todos los datos obligatorios
     * @throws IllegalStateException si solo se definió una parte de los datos obligatorios
     */
    public boolean isConfigured() {
        long defined = Stream.of(email, password, idNumber).filter(AdminBootstrapProperties::hasText).count();
        if (defined > 0 && defined < 3) {
            throw new IllegalStateException(
                    "Para crear el administrador inicial se requieren ADMIN_EMAIL, ADMIN_PASSWORD y ADMIN_ID_NUMBER");
        }
        return defined == 3;
    }

    @Override
    public String toString() {
        return "AdminBootstrapProperties[configured=" + (hasText(email) && hasText(password)) + "]";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
