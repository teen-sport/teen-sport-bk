package com.uniquindio.teen_sport.repository;

import java.time.LocalDate;

/**
 * Filtros del historial de pagos (E19). Un valor {@code null} no filtra.
 *
 * @param enrollmentId matrícula
 * @param athleteId    deportista
 * @param from         fecha de pago mínima, incluida
 * @param to           fecha de pago máxima, incluida
 * @param guardianId   acudiente del deportista; lo fija el servicio según el rol
 */
public record PaymentFilter(Integer enrollmentId, Integer athleteId, LocalDate from, LocalDate to,
                            Integer guardianId) {

    /**
     * @param scope acudiente al que se limita la consulta, o {@code null} para no limitarla
     * @return los mismos filtros con ese acudiente
     */
    public PaymentFilter withGuardian(Integer scope) {
        return new PaymentFilter(enrollmentId, athleteId, from, to, scope);
    }
}
