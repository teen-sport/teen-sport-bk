package com.uniquindio.teen_sport.repository;

/**
 * Cupos ocupados de un grupo, calculados en la base de datos (proyección de
 * {@link EnrollmentRepository#countOccupancy}).
 */
public interface GroupOccupancy {

    /**
     * @return id del grupo
     */
    Integer getGroupId();

    /**
     * @return matrículas vigentes que empezaron en el periodo consultado o antes
     */
    Long getStartedBy();

    /**
     * @return todas las matrículas vigentes del grupo, sin importar su mes de inicio
     */
    Long getInForce();
}
