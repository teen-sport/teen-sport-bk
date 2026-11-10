package com.uniquindio.teen_sport.repository;

import java.time.Instant;

/**
 * Momentos de solicitud y confirmación de una matrícula, para el tiempo promedio de confirmación (R1).
 */
public interface ConfirmationTime {

    /**
     * @return momento de la solicitud
     */
    Instant getCreatedAt();

    /**
     * @return momento de la confirmación
     */
    Instant getConfirmedAt();
}
