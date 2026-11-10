package com.uniquindio.teen_sport.repository;

/**
 * Sesiones programadas y realizadas de un grupo en un rango de fechas (R2).
 */
public interface GroupSessionCount {

    /**
     * @return id del grupo
     */
    Integer getGroupId();

    /**
     * @return nombre del grupo
     */
    String getGroupName();

    /**
     * @return nombre de la disciplina del grupo
     */
    String getDiscipline();

    /**
     * @return sesiones no canceladas
     */
    Long getScheduled();

    /**
     * @return sesiones con la asistencia registrada ({@code COMPLETED})
     */
    Long getCompleted();
}
