package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.EnrollmentReport;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import com.uniquindio.teen_sport.repository.ConfirmationTime;
import com.uniquindio.teen_sport.repository.DisciplineEnrollmentCount;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.EnrollmentStatusCount;
import com.uniquindio.teen_sport.repository.PaymentStatusCount;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * R1: informe de matrículas.
 */
class EnrollmentReportServiceTest {

    private static final YearMonth PERIOD = YearMonth.of(2026, 11);
    private static final String MONTH = "2026-11";
    private static final Instant CREATED = Instant.parse("2026-11-01T10:00:00Z");

    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private EnrollmentReportService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentReportService(enrollmentRepository);
    }

    @Test
    void computesCountsRatesAverageAndPayments() {
        List<EnrollmentStatusCount> statuses = List.of(statusCount(EnrollmentStatus.ACTIVE, 2),
                statusCount(EnrollmentStatus.PENDING_REVIEW, 3), statusCount(EnrollmentStatus.CANCELLED, 1));
        when(enrollmentRepository.countByStatusForPeriod(MONTH)).thenReturn(statuses);
        DisciplineEnrollmentCount football = mock(DisciplineEnrollmentCount.class);
        when(football.getDisciplineId()).thenReturn(4);
        when(football.getName()).thenReturn("Fútbol");
        when(football.getRequests()).thenReturn(6L);
        when(football.getActive()).thenReturn(2L);
        when(enrollmentRepository.countByDisciplineForPeriod(MONTH)).thenReturn(List.of(football));
        List<ConfirmationTime> times = List.of(confirmation(Duration.ofHours(10)),
                confirmation(Duration.ofHours(27).plusMinutes(3)));
        when(enrollmentRepository.findConfirmationTimes(MONTH)).thenReturn(times);
        List<PaymentStatusCount> payments = List.of(paymentCount(PaymentStatus.OVERDUE, 5));
        when(enrollmentRepository.countActivePaymentStatusInForce(MONTH)).thenReturn(payments);

        EnrollmentReport report = service.enrollmentReport(PERIOD);

        assertThat(report.period()).isEqualTo(PERIOD);
        assertThat(report.totalRequests()).isEqualTo(6);
        assertThat(report.byStatus()).containsExactly(
                entry(EnrollmentStatus.PENDING_REVIEW, 3L), entry(EnrollmentStatus.ACTIVE, 2L),
                entry(EnrollmentStatus.REJECTED, 0L), entry(EnrollmentStatus.CANCELLED, 1L));
        assertThat(report.completionRate()).isEqualTo(33.3);
        assertThat(report.abandonmentRate()).isEqualTo(16.7);
        assertThat(report.averageConfirmationHours()).isEqualTo(18.5);
        assertThat(report.byDiscipline())
                .containsExactly(new EnrollmentReport.DisciplineEnrollments(4, "Fútbol", 6, 2));
        assertThat(report.payments()).isEqualTo(new EnrollmentReport.PaymentTotals(0, 5));
    }

    @Test
    void periodWithoutEnrollmentsHasZeroRatesAndNoAverage() {
        EnrollmentReport report = service.enrollmentReport(PERIOD);

        assertThat(report.totalRequests()).isZero();
        assertThat(report.byStatus()).hasSize(EnrollmentStatus.values().length).containsValues(0L);
        assertThat(report.completionRate()).isZero();
        assertThat(report.abandonmentRate()).isZero();
        assertThat(report.averageConfirmationHours()).isNull();
        assertThat(report.byDiscipline()).isEmpty();
        assertThat(report.payments()).isEqualTo(new EnrollmentReport.PaymentTotals(0, 0));
    }

    private static java.util.Map.Entry<EnrollmentStatus, Long> entry(EnrollmentStatus status, long total) {
        return java.util.Map.entry(status, total);
    }

    private static EnrollmentStatusCount statusCount(EnrollmentStatus status, long total) {
        EnrollmentStatusCount count = mock(EnrollmentStatusCount.class);
        when(count.getStatus()).thenReturn(status);
        when(count.getTotal()).thenReturn(total);
        return count;
    }

    private static PaymentStatusCount paymentCount(PaymentStatus status, long total) {
        PaymentStatusCount count = mock(PaymentStatusCount.class);
        when(count.getStatus()).thenReturn(status);
        when(count.getTotal()).thenReturn(total);
        return count;
    }

    private static ConfirmationTime confirmation(Duration elapsed) {
        ConfirmationTime time = mock(ConfirmationTime.class);
        when(time.getCreatedAt()).thenReturn(CREATED);
        when(time.getConfirmedAt()).thenReturn(CREATED.plus(elapsed));
        return time;
    }
}
