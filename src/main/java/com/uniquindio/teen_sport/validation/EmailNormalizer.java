package com.uniquindio.teen_sport.validation;

import java.util.Locale;

/**
 * Forma canónica de los correos: sin espacios alrededor y en minúsculas.
 *
 * <p>Se aplica antes de guardar y antes de buscar, para que la unicidad del correo y el login no
 * dependan de mayúsculas o espacios accidentales.</p>
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    /**
     * @param email correo tal como llegó
     * @return correo normalizado
     */
    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
