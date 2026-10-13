package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T17:00:00Z");
    private static final YearMonth START = YearMonth.of(2026, 11);

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, RoleName.ADMIN, null, null);
    private static final AuthenticatedUser GUARDIAN = new AuthenticatedUser(2, RoleName.GUARDIAN, 20, null);
    private static final AuthenticatedUser OTHER_GUARDIAN = new AuthenticatedUser(3, RoleName.GUARDIAN, 30, null);

    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private EnrollmentLifecycleService service;
    private Discipline football;

    @BeforeEach
    void setUp() {
        AthleteAccessPolicy accessPolicy = new AthleteAccessPolicy();
        service = new EnrollmentLifecycleService(enrollmentRepository, groupRepository, accessPolicy,
                new SpotCounter(enrollmentRepository),
                new EnrollmentQueryService(enrollmentRepository, scheduleRepository, accessPolicy),
                new AcademyCalendar(Clock.fixed(NOW, ZoneOffset.UTC), "America/Bogota"));
        football = discipline(3);
    }

    @Test
    void confirmActivatesTheEnrollmentKeepingPaymentOverdue() {
        Enrollment enrollment = enrollment(9, group(7, football, 15), EnrollmentStatus.PENDING_REVIEW);

        EnrollmentDetail detail = service.confirm(9, null);

        assertThat(detail.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(detail.confirmedAt()).isEqualTo(NOW);
        assertThat(detail.paymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(groupRepository, never()).findLockedById(any());
    }

    @Test
    void confirmInTheCurrentGroupRequiresItToBeActive() {
        Group group = group(7, football, 15);
        group.setStatus(EntityStatus.INACTIVE);
        enrollment(9, group, EnrollmentStatus.PENDING_REVIEW);

        assertThat(failure(() -> service.confirm(9, 7)).getCode()).isEqualTo(ErrorCode.GROUP_INACTIVE);
    }

    @Test
    void confirmCanMoveToAnotherGroupOfTheSameDisciplineLockingItFirst() {
        Enrollment enrollment = enrollment(9, group(7, football, 15), EnrollmentStatus.PENDING_REVIEW);
        Group target = group(8, football, 15);

        EnrollmentDetail detail = service.confirm(9, 8);

        assertThat(detail.group().id()).isEqualTo(8);
        assertThat(enrollment.getGroup()).isSameAs(target);
        assertThat(enrollment.getPeriod()).isEqualTo(START);
        InOrder order = inOrder(enrollmentRepository, groupRepository);
        order.verify(enrollmentRepository).findLockedById(9);
        order.verify(groupRepository).findLockedById(8);
        order.verify(enrollmentRepository).countByGroupIdAndStatusIn(8, EnrollmentStatus.IN_FORCE);
    }

    @Test
    void movingRequiresSameDisciplineActiveGroupAndAFreeSpot() {
        Enrollment enrollment = enrollment(9, group(7, football, 15), EnrollmentStatus.ACTIVE);
        group(20, discipline(4), 15);
        Group inactive = group(21, football, 15);
        inactive.setStatus(EntityStatus.INACTIVE);
        group(22, football, 2);
        when(enrollmentRepository.countByGroupIdAndStatusIn(22, EnrollmentStatus.IN_FORCE)).thenReturn(2L);

        assertThat(failure(() -> service.changeGroup(9, 99)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.changeGroup(9, 20)).getCode()).isEqualTo(ErrorCode.GROUP_OTHER_DISCIPLINE);
        assertThat(failure(() -> service.changeGroup(9, 21)).getCode()).isEqualTo(ErrorCode.GROUP_INACTIVE);
        BusinessException full = failure(() -> service.changeGroup(9, 22));
        assertThat(full.getCode()).isEqualTo(ErrorCode.NO_AVAILABLE_SPOTS);
        assertThat(full.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(enrollment.getGroup().getId()).isEqualTo(7);
    }

    @Test
    void changeGroupKeepsStartPeriodAndStatus() {
        Enrollment enrollment = enrollment(9, group(7, football, 15), EnrollmentStatus.ACTIVE);
        group(8, football, 15);

        EnrollmentDetail detail = service.changeGroup(9, 8);

        assertThat(detail.group().id()).isEqualTo(8);
        assertThat(detail.period()).isEqualTo(START);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    void changingToTheSameGroupChangesNothing() {
        enrollment(9, group(7, football, 15), EnrollmentStatus.PENDING_REVIEW);

        assertThat(service.changeGroup(9, 7).group().id()).isEqualTo(7);
        verify(groupRepository, never()).findLockedById(any());
    }

    @ParameterizedTest
    @EnumSource(value = EnrollmentStatus.class, names = {"ACTIVE", "REJECTED", "CANCELLED"})
    void onlyPendingEnrollmentsCanBeConfirmedOrRejected(EnrollmentStatus status) {
        enrollment(9, group(7, football, 15), status);

        BusinessException confirm = failure(() -> service.confirm(9, null));
        assertThat(confirm.getCode()).isEqualTo(ErrorCode.INVALID_STATE);
        assertThat(confirm.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(failure(() -> service.reject(9, "Motivo")).getCode()).isEqualTo(ErrorCode.INVALID_STATE);
    }

    @ParameterizedTest
    @EnumSource(value = EnrollmentStatus.class, names = {"REJECTED", "CANCELLED"})
    void finishedEnrollmentsCannotBeCancelledOrMoved(EnrollmentStatus status) {
        enrollment(9, group(7, football, 15), status);

        assertThat(failure(() -> service.cancel(ADMIN, 9, null)).getCode()).isEqualTo(ErrorCode.INVALID_STATE);
        assertThat(failure(() -> service.changeGroup(9, 8)).getCode()).isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void rejectStoresTheReason() {
        Enrollment enrollment = enrollment(9, group(7, football, 15), EnrollmentStatus.PENDING_REVIEW);

        EnrollmentDetail detail = service.reject(9, "  Documentos incompletos ");

        assertThat(detail.status()).isEqualTo(EnrollmentStatus.REJECTED);
        assertThat(detail.reviewNotes()).isEqualTo("Documentos incompletos");
        assertThat(enrollment.getConfirmedAt()).isNull();
    }

    @Test
    void guardianCancelsOwnPendingEnrollmentWithOptionalReason() {
        Enrollment enrollment = enrollment(9, group(7, football, 15), EnrollmentStatus.PENDING_REVIEW);

        EnrollmentDetail detail = service.cancel(GUARDIAN, 9, " Cambio de horario ");

        assertThat(detail.status()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getReviewNotes()).isEqualTo("Cambio de horario");
    }

    @Test
    void blankReasonKeepsThePreviousNotes() {
        Enrollment enrollment = enrollment(9, group(7, football, 15), EnrollmentStatus.ACTIVE);
        enrollment.setReviewNotes("Nota previa");

        service.cancel(ADMIN, 9, "   ");

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getReviewNotes()).isEqualTo("Nota previa");
    }

    @Test
    void guardianCannotCancelAnActiveOrAnotherGuardiansEnrollment() {
        Enrollment active = enrollment(9, group(7, football, 15), EnrollmentStatus.ACTIVE);

        BusinessException own = failure(() -> service.cancel(GUARDIAN, 9, null));
        assertThat(own.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(own.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(failure(() -> service.cancel(OTHER_GUARDIAN, 9, null)).getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(active.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);

        assertThat(service.cancel(ADMIN, 9, null).status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void missingEnrollmentIsNotFound() {
        assertThat(failure(() -> service.confirm(99, null)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.cancel(ADMIN, 99, null)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Enrollment enrollment(int id, Group group, EnrollmentStatus status) {
        Guardian guardian = new Guardian(new User(new Role(RoleName.GUARDIAN, null), "g@teensport.test", "$2a$hash",
                NOW), null);
        ReflectionTestUtils.setField(guardian, "id", 20);
        Athlete athlete = new Athlete(guardian, NOW);
        ReflectionTestUtils.setField(athlete, "id", 5);
        Enrollment enrollment = new Enrollment(athlete, group, START, NOW);
        enrollment.setStatus(status);
        ReflectionTestUtils.setField(enrollment, "id", id);
        when(enrollmentRepository.findLockedById(id)).thenReturn(Optional.of(enrollment));
        return enrollment;
    }

    private Group group(int id, Discipline discipline, int capacity) {
        Group group = new Group(discipline, null, "Grupo " + id, capacity);
        ReflectionTestUtils.setField(group, "id", id);
        when(groupRepository.findLockedById(id)).thenReturn(Optional.of(group));
        return group;
    }

    private static Discipline discipline(int id) {
        Discipline discipline = new Discipline("Disciplina " + id, null, BigDecimal.TEN);
        ReflectionTestUtils.setField(discipline, "id", id);
        return discipline;
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
