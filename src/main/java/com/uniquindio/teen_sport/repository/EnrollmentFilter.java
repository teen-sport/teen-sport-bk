package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import java.time.YearMonth;

/**
 * Filtros del listado de matrículas (E7). Un valor {@code null} no filtra.
 *
 * @param status          estado
 * @param paymentStatus   estado de pago
 * @param paymentReported si el acudiente reportó un pago
 * @param period          mes en el que la matrícula está vigente (ver {@link EnrollmentSpecifications})
 * @param disciplineId    disciplina del grupo
 * @param groupId         grupo
 * @param athleteId       deportista
 * @param guardianId      acudiente del deportista
 */
public record EnrollmentFilter(EnrollmentStatus status, PaymentStatus paymentStatus, Boolean paymentReported,
                               YearMonth period, Integer disciplineId, Integer groupId, Integer athleteId,
                               Integer guardianId) {

    /**
     * @param scope acudiente al que se limita la consulta, o {@code null} para no limitarla
     * @return los mismos filtros con ese acudiente
     */
    public EnrollmentFilter withGuardian(Integer scope) {
        return new EnrollmentFilter(status, paymentStatus, paymentReported, period, disciplineId, groupId,
                athleteId, scope);
    }
}
