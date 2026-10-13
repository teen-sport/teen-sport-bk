package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

/**
 * Detalle de una matrícula con las franjas de su grupo (esquema {@code EnrollmentDetail}: los campos
 * de {@code EnrollmentSummary} más {@code schedules}, {@code reviewNotes} y {@code confirmedAt}).
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
 * @param schedules       franjas del grupo, ordenadas por día y hora
 * @param reviewNotes     último mensaje o motivo escrito por el administrador
 * @param confirmedAt     fecha de confirmación
 */
public record EnrollmentDetail(Integer id, AthleteRef athlete, IdName group, IdName discipline,
                               @JsonFormat(pattern = "yyyy-MM") YearMonth period, EnrollmentStatus status,
                               PaymentStatus paymentStatus, boolean paymentReported, Instant createdAt,
                               List<ScheduleResponse> schedules, String reviewNotes, Instant confirmedAt) {
}
