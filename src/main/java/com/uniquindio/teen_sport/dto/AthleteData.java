package com.uniquindio.teen_sport.dto;

import java.time.LocalDate;

/**
 * Datos personales de un deportista, comunes al registro (E1) y a la edición (E4).
 */
public interface AthleteData {

    /**
     * @return nombres
     */
    String firstNames();

    /**
     * @return apellidos
     */
    String lastNames();

    /**
     * @return tipo de documento
     */
    String idType();

    /**
     * @return número de documento
     */
    String idNumber();

    /**
     * @return fecha de nacimiento
     */
    LocalDate birthDate();

    /**
     * @return género, opcional
     */
    String gender();
}
