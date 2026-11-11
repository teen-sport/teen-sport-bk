package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.EnrollmentStatus;

/**
 * Número de matrículas en un estado (R1).
 */
public interface EnrollmentStatusCount {

    /**
     * @return estado de la matrícula
     */
    EnrollmentStatus getStatus();

    /**
     * @return matrículas en ese estado
     */
    Long getTotal();
}
