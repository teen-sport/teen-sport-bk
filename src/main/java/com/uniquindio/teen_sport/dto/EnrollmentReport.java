package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Indicadores de matrícula de un periodo (R1).
 *
 * @param period                   mes de inicio de las matrículas contadas, {@code YYYY-MM}
 * @param totalRequests            matrículas solicitadas con ese mes de inicio
 * @param byStatus                 matrículas por estado; incluye todos los estados, aunque sean cero
 * @param completionRate           porcentaje {@code ACTIVE} sobre {@code totalRequests}
 * @param abandonmentRate          porcentaje {@code CANCELLED} sobre {@code totalRequests}
 * @param averageConfirmationHours horas promedio entre la solicitud y la confirmación de las
 *                                 matrículas {@code ACTIVE}; {@code null} si no hay ninguna
 * @param byDiscipline             solicitudes y activas por disciplina
 * @param payments                 estado de pago de las matrículas activas vigentes en el periodo
 */
public record EnrollmentReport(@JsonFormat(pattern = "yyyy-MM") YearMonth period, long totalRequests,
                               Map<EnrollmentStatus, Long> byStatus, double completionRate,
                               double abandonmentRate, Double averageConfirmationHours,
                               List<DisciplineEnrollments> byDiscipline, PaymentTotals payments) {

    /**
     * @param disciplineId id de la disciplina
     * @param name         nombre de la disciplina
     * @param requests     matrículas solicitadas en el periodo
     * @param active       matrículas {@code ACTIVE}
     */
    public record DisciplineEnrollments(Integer disciplineId, String name, long requests, long active) {
    }

    /**
     * @param upToDate matrículas al día
     * @param overdue  matrículas en mora
     */
    public record PaymentTotals(long upToDate, long overdue) {
    }
}
