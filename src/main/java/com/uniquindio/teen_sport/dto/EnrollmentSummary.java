package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import java.time.Instant;
import java.time.YearMonth;

/**
 * Matrícula en los listados (esquema {@code EnrollmentSummary}).
 *
 * @param id              id
 * @param athlete         deportista
 * @param group           grupo
 * @param discipline      disciplina del grupo
 * @param period          mes de inicio, {@code YYYY-MM}
 * @param status          estado
 * @param paymentStatus   estado de pago
 * @param paymentReported si el acudiente reportó un pago
 * @param createdAt       fecha de creación
 */
public record EnrollmentSummary(Integer id, AthleteRef athlete, IdName group, IdName discipline,
                                @JsonFormat(pattern = "yyyy-MM") YearMonth period, EnrollmentStatus status,
                                PaymentStatus paymentStatus, boolean paymentReported, Instant createdAt) {
}
