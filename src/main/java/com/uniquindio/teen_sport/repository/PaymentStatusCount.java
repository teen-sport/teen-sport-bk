package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.PaymentStatus;

/**
 * Número de matrículas con un estado de pago (R1).
 */
public interface PaymentStatusCount {

    /**
     * @return estado de pago
     */
    PaymentStatus getStatus();

    /**
     * @return matrículas con ese estado de pago
     */
    Long getTotal();
}
