package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.dto.PaymentReportRequest;
import com.uniquindio.teen_sport.dto.PaymentResponse;
import com.uniquindio.teen_sport.dto.RegisterPaymentRequest;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.PaymentHistory;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.PaymentHistoryRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    /** 13 de octubre en UTC, pero todavía 12 de octubre en Bogotá. */
    private static final Instant NOW = Instant.parse("2026-10-13T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final BigDecimal FEE = new BigDecimal("120000.00");

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, RoleName.ADMIN, null, null);
    private static final AuthenticatedUser GUARDIAN = new AuthenticatedUser(2, RoleName.GUARDIAN, 20, null);
    private static final AuthenticatedUser OTHER_GUARDIAN = new AuthenticatedUser(3, RoleName.GUARDIAN, 30, null);

    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final PaymentHistoryRepository paymentRepository = mock(PaymentHistoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private PaymentService service;

    @BeforeEach
    void setUp() {
        AthleteAccessPolicy accessPolicy = new AthleteAccessPolicy();
        AcademyCalendar calendar = new AcademyCalendar(Clock.fixed(NOW, ZoneOffset.UTC), "America/Bogota");
        service = new PaymentService(enrollmentRepository, accessPolicy,
                new PaymentRecorder(paymentRepository, userRepository, calendar),
                new EnrollmentQueryService(enrollmentRepository, mock(ScheduleRepository.class), accessPolicy),
                calendar);
        User admin = new User(new Role(RoleName.ADMIN, null), "admin@teensport.test", "$2a$hash", NOW);
        admin.setFirstName("Ana");
        admin.setLastName("Pérez");
        ReflectionTestUtils.setField(admin, "id", 1);
        when(userRepository.getReferenceById(1)).thenReturn(admin);
        when(paymentRepository.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void guardianReportOnlyMarksTheEnrollment() {
        Enrollment enrollment = enrollment(EnrollmentStatus.ACTIVE);

        EnrollmentDetail detail = service.report(GUARDIAN, 9,
                new PaymentReportRequest(LocalDate.of(2026, 9, 30), "  Pagué en efectivo "));

        assertThat(detail.paymentReported()).isTrue();
        assertThat(detail.paymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
        assertThat(enrollment.getPaymentReportedAt()).isEqualTo(NOW);
        assertThat(enrollment.getPaymentReportNote()).isEqualTo("Pagué en efectivo");
        verify(paymentRepository).existsByEnrollmentIdAndPaymentDateBetween(9, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void reportWithoutDateChecksTheCurrentMonthOfTheAcademy() {
        enrollment(EnrollmentStatus.PENDING_REVIEW);

        service.report(GUARDIAN, 9, null);

        verify(paymentRepository).existsByEnrollmentIdAndPaymentDateBetween(9, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31));
    }

    @Test
    void reportForAMonthAlreadyPaidIsAConflict() {
        Enrollment enrollment = enrollment(EnrollmentStatus.ACTIVE);
        monthAlreadyPaid(YearMonth.of(2026, 10));

        BusinessException failure = failure(() -> service.report(GUARDIAN, 9, new PaymentReportRequest(TODAY, null)));

        assertThat(failure.getCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_REGISTERED);
        assertThat(failure.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(enrollment.isPaymentReported()).isFalse();
    }

    @Test
    void onlyTheOwnerGuardianReports() {
        Enrollment enrollment = enrollment(EnrollmentStatus.ACTIVE);

        assertThat(failure(() -> service.report(OTHER_GUARDIAN, 9, null)).getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(failure(() -> service.report(ADMIN, 9, null)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(enrollment.isPaymentReported()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EnrollmentStatus.class, names = {"REJECTED", "CANCELLED"})
    void finishedEnrollmentsAcceptNoReportsOrPayments(EnrollmentStatus status) {
        enrollment(status);

        assertThat(failure(() -> service.report(GUARDIAN, 9, null)).getCode()).isEqualTo(ErrorCode.INVALID_STATE);
        assertThat(failure(() -> service.register(ADMIN, 9, payment(FEE, TODAY))).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void registerSavesThePaymentAndLeavesTheEnrollmentUpToDate() {
        Enrollment enrollment = enrollment(EnrollmentStatus.PENDING_REVIEW);
        enrollment.setPaymentReported(true);
        enrollment.setPaymentReportNote("Pagué");

        PaymentResponse response = service.register(ADMIN, 9,
                new RegisterPaymentRequest(FEE, TODAY, " R-001 ", "  "));

        ArgumentCaptor<PaymentHistory> saved = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(response.amountPaid()).isEqualByComparingTo(FEE);
        assertThat(response.period()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(response.receiptReference()).isEqualTo("R-001");
        assertThat(response.notes()).isNull();
        assertThat(response.registeredBy().id()).isEqualTo(1);
        assertThat(response.registeredBy().fullName()).isEqualTo("Ana Pérez");
        assertThat(response.athlete().id()).isEqualTo(5);
        assertThat(response.discipline().id()).isEqualTo(3);
        assertThat(enrollment.getPaymentStatus()).isEqualTo(PaymentStatus.UP_TO_DATE);
        assertThat(enrollment.isPaymentReported()).isFalse();
        assertThat(enrollment.getPaymentReportNote()).isEqualTo("Pagué");
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING_REVIEW);
    }

    @Test
    void amountMustReachTheMonthlyFee() {
        enrollment(EnrollmentStatus.ACTIVE);

        BusinessException failure = failure(() -> service.register(ADMIN, 9,
                payment(new BigDecimal("119999.99"), TODAY)));

        assertThat(failure.getCode()).isEqualTo(ErrorCode.INSUFFICIENT_AMOUNT);
        assertThat(failure.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(service.register(ADMIN, 9, payment(new BigDecimal("150000"), TODAY)).amountPaid())
                .isEqualByComparingTo("150000");
    }

    @Test
    void paymentDateCannotBeAfterTodayInTheAcademyZone() {
        enrollment(EnrollmentStatus.ACTIVE);

        BusinessException failure = failure(() -> service.register(ADMIN, 9, payment(FEE, TODAY.plusDays(1))));

        assertThat(failure.getCode()).isEqualTo(ErrorCode.FUTURE_DATE);
        assertThat(failure.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void secondPaymentInTheSameMonthIsAConflict() {
        Enrollment enrollment = enrollment(EnrollmentStatus.ACTIVE);
        monthAlreadyPaid(YearMonth.of(2026, 9));

        BusinessException failure = failure(() -> service.register(ADMIN, 9, payment(FEE, LocalDate.of(2026, 9, 5))));

        assertThat(failure.getCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_REGISTERED);
        assertThat(enrollment.getPaymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
        assertThat(service.register(ADMIN, 9, payment(FEE, TODAY)).period()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void markOverdueWorksInAnyEnrollmentStatus() {
        Enrollment enrollment = enrollment(EnrollmentStatus.CANCELLED);
        enrollment.setPaymentStatus(PaymentStatus.UP_TO_DATE);

        assertThat(service.markOverdue(9, PaymentStatus.OVERDUE).paymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
        assertThat(service.markOverdue(9, PaymentStatus.OVERDUE).paymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
    }

    @Test
    void upToDateIsOnlyReachedByRegisteringAPayment() {
        BusinessException failure = failure(() -> service.markOverdue(9, PaymentStatus.UP_TO_DATE));

        assertThat(failure.getCode()).isEqualTo(ErrorCode.USE_PAYMENT_REGISTRATION);
        assertThat(failure.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void missingEnrollmentIsNotFound() {
        when(enrollmentRepository.findLockedById(anyInt())).thenReturn(Optional.empty());

        assertThat(failure(() -> service.report(GUARDIAN, 99, null)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.register(ADMIN, 99, payment(FEE, TODAY))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.markOverdue(99, PaymentStatus.OVERDUE)).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void monthAlreadyPaid(YearMonth month) {
        when(paymentRepository.existsByEnrollmentIdAndPaymentDateBetween(9, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(true);
    }

    private Enrollment enrollment(EnrollmentStatus status) {
        Guardian guardian = new Guardian(new User(new Role(RoleName.GUARDIAN, null), "g@teensport.test", "$2a$hash",
                NOW), null);
        ReflectionTestUtils.setField(guardian, "id", 20);
        Athlete athlete = new Athlete(guardian, NOW);
        ReflectionTestUtils.setField(athlete, "id", 5);
        Discipline discipline = new Discipline("Fútbol", null, FEE);
        ReflectionTestUtils.setField(discipline, "id", 3);
        Group group = new Group(discipline, null, "Grupo 7", 15);
        ReflectionTestUtils.setField(group, "id", 7);
        Enrollment enrollment = new Enrollment(athlete, group, YearMonth.of(2026, 9), NOW);
        enrollment.setStatus(status);
        ReflectionTestUtils.setField(enrollment, "id", 9);
        when(enrollmentRepository.findLockedById(9)).thenReturn(Optional.of(enrollment));
        return enrollment;
    }

    private static RegisterPaymentRequest payment(BigDecimal amount, LocalDate date) {
        return new RegisterPaymentRequest(amount, date, null, null);
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
