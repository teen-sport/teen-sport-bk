package com.uniquindio.teen_sport.dto;

/**
 * Datos editables de un perfil, comunes a la edición propia (U4) y a la edición por un
 * administrador (U9).
 */
public interface EditableProfile {

    /**
     * @return nombres
     */
    String firstNames();

    /**
     * @return apellidos
     */
    String lastNames();

    /**
     * @return teléfono
     */
    String phone();

    /**
     * @return dirección; solo acudientes
     */
    String homeAddress();

    /**
     * @return especialidad; solo entrenadores
     */
    String specialty();

    /**
     * @return años de experiencia; solo entrenadores
     */
    Integer yearsOfExperience();
}
