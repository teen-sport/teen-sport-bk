package com.uniquindio.teen_sport.repository;

/**
 * Marcas de asistencia de un grupo (R2, R3).
 */
public interface GroupMarkCount {

    /**
     * @return id del grupo
     */
    Integer getGroupId();

    /**
     * @return nombre del grupo
     */
    String getGroupName();

    /**
     * @return marcas {@code PRESENT}
     */
    Long getPresent();

    /**
     * @return marcas {@code ABSENT}
     */
    Long getAbsent();
}
