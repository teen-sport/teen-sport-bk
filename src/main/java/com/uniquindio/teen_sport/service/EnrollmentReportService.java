package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.EnrollmentReport;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import com.uniquindio.teen_sport.repository.ConfirmationTime;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.EnrollmentStatusCount;
import com.uniquindio.teen_sport.repository.PaymentStatusCount;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Informe de matrículas (R1), con los KPI del Proceso 1: inscripciones por periodo, tiempo promedio
 * de confirmación y porcentaje de matrículas completas frente a abandonadas.
 *
 * <p>Reglas (decisión del 2026-11-10):</p>
 * <ul>
 *   <li>Las matrículas del periodo son las que tienen ese mes de inicio ({@code period_month}).</li>
 *   <li>{@code completionRate} = {@code ACTIVE} / total y {@code abandonmentRate} = {@code CANCELLED} /
 *       total; sin solicitudes valen 0.</li>
 *   <li>{@code averageConfirmationHours} promedia {@code confirmedAt - createdAt} de las matrículas
 *       {@code ACTIVE} del periodo; sin ninguna es {@code null}.</li>
 *   <li>{@code payments} cuenta el estado de pago actual de las matrículas {@code ACTIVE} vigentes en el
 *       periodo (iniciadas ese mes o antes), porque el estado de pago no es histórico.</li>
 * </ul>
 */
@Service
public class EnrollmentReportService {

    private static final double SECONDS_PER_HOUR = 3600.0;

    private final EnrollmentRepository enrollmentRepository;

    /**
     * @param enrollmentRepository matrículas
     */
    public EnrollmentReportService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * R1: indicadores de matrícula del periodo.
     *
     * @param period mes de inicio de las matrículas
     * @return el informe
     */
    @Transactional(readOnly = true)
    public EnrollmentReport enrollmentReport(YearMonth period) {
        String month = period.toString();
        Map<EnrollmentStatus, Long> byStatus = countByStatus(enrollmentRepository.countByStatusForPeriod(month));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        List<EnrollmentReport.DisciplineEnrollments> byDiscipline = enrollmentRepository
                .countByDisciplineForPeriod(month).stream()
                .map(count -> new EnrollmentReport.DisciplineEnrollments(count.getDisciplineId(), count.getName(),
                        count.getRequests(), count.getActive()))
                .toList();
        return new EnrollmentReport(period, total, byStatus,
                Percentages.of(byStatus.get(EnrollmentStatus.ACTIVE), total),
                Percentages.of(byStatus.get(EnrollmentStatus.CANCELLED), total),
                averageConfirmationHours(enrollmentRepository.findConfirmationTimes(month)),
                byDiscipline,
                paymentTotals(enrollmentRepository.countActivePaymentStatusInForce(month)));
    }

    private static Map<EnrollmentStatus, Long> countByStatus(List<EnrollmentStatusCount> counts) {
        Map<EnrollmentStatus, Long> byStatus = new EnumMap<>(EnrollmentStatus.class);
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
            byStatus.put(status, 0L);
        }
        counts.forEach(count -> byStatus.put(count.getStatus(), count.getTotal()));
        return Collections.unmodifiableMap(byStatus);
    }

    private static Double averageConfirmationHours(List<ConfirmationTime> times) {
        if (times.isEmpty()) {
            return null;
        }
        double averageSeconds = times.stream()
                .mapToLong(time -> Duration.between(time.getCreatedAt(), time.getConfirmedAt()).toSeconds())
                .average()
                .orElseThrow();
        return Percentages.round(averageSeconds / SECONDS_PER_HOUR);
    }

    private static EnrollmentReport.PaymentTotals paymentTotals(List<PaymentStatusCount> counts) {
        Map<PaymentStatus, Long> byStatus = new EnumMap<>(PaymentStatus.class);
        counts.forEach(count -> byStatus.put(count.getStatus(), count.getTotal()));
        return new EnrollmentReport.PaymentTotals(byStatus.getOrDefault(PaymentStatus.UP_TO_DATE, 0L),
                byStatus.getOrDefault(PaymentStatus.OVERDUE, 0L));
    }
}
