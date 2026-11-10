package com.uniquindio.teen_sport.repository;

/**
 * Solicitudes de matrícula de una disciplina en un periodo (R1).
 */
public interface DisciplineEnrollmentCount {

    /**
     * @return id de la disciplina
     */
    Integer getDisciplineId();

    /**
     * @return nombre de la disciplina
     */
    String getName();

    /**
     * @return matrículas solicitadas
     */
    Long getRequests();

    /**
     * @return matrículas en estado {@code ACTIVE}
     */
    Long getActive();
}
