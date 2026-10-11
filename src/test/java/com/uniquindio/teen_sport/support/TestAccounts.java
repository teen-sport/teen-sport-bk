package com.uniquindio.teen_sport.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Datos ficticios para las pruebas. Ninguno corresponde a personas reales.
 */
public final class TestAccounts {

    /** Secreto JWT exclusivo de pruebas (más de 32 bytes). */
    public static final String JWT_SECRET = "teen-sport-test-secret-only-for-automated-tests-0001";

    /** Administrador inicial que crea el arranque en las pruebas de integración. */
    public static final String ADMIN_EMAIL = "admin@teensport.test";
    public static final String ADMIN_PASSWORD = "Admin#Test2026";
    public static final String ADMIN_ID_NUMBER = "900000001";

    /** Contraseña que cumple la política. */
    public static final String VALID_PASSWORD = "Segura#2026";

    private TestAccounts() {
    }

    /**
     * @return correo único, para no chocar con datos de otras pruebas
     */
    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@teensport.test";
    }

    /**
     * @return número de documento único de 12 dígitos
     */
    public static String uniqueIdNumber() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L));
    }

    /**
     * @param email    correo del acudiente
     * @param idNumber documento del acudiente
     * @param password contraseña
     * @return cuerpo JSON de {@code POST /users/register} con la política de datos aceptada
     */
    public static String registrationJson(String email, String idNumber, String password) {
        return """
                {"firstNames":"Ana María","lastNames":"Prueba Test","idType":"CC","idNumber":"%s",
                 "email":"%s","phone":"3001234567","homeAddress":"Calle 1 # 2-3",
                 "password":"%s","acceptedDataPolicy":true}
                """.formatted(idNumber, email, password);
    }
}
