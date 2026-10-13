package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.CreateEnrollmentRequest;
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
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.AthleteRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class EnrollmentServiceTest {

    /** 12 de octubre de 2026 a mediodía en Bogotá. */
    private static final Instant NOW = Instant.parse("2026-10-12T17:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final YearMonth CURRENT = YearMonth.of(2026, 10);

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, RoleName.ADMIN, null, null);
    private static final AuthenticatedUser GUARDIAN = new AuthenticatedUser(2, RoleName.GUARDIAN, 20, null);
    private static final AuthenticatedUser OTHER_GUARDIAN = new AuthenticatedUser(3, RoleName.GUARDIAN, 30, null);

    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final AthleteRepository athleteRepository = mock(AthleteRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        AthleteAccessPolicy accessPolicy = new AthleteAccessPolicy();
        service = new EnrollmentService(enrollmentRepository, athleteRepository, groupRepository, accessPolicy,
                new SpotCounter(enrollmentRepository),
                new EnrollmentQueryService(enrollmentRepository, scheduleRepository, accessPolicy),
                new AcademyCalendar(Clock.fixed(NOW, ZoneOffset.UTC), "America/Bogota"));
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsPendingEnrollmentWithOverduePaymentAndGroupSchedules() {
        Athlete athlete = athlete(5, TODAY.minusYears(14));
        Group group = group(7, 15, EntityStatus.ACTIVE, EntityStatus.ACTIVE);
        Schedule schedule = new Schedule(group);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(16, 0));
        schedule.setEndTime(LocalTime.of(18, 0));
        when(scheduleRepository.findByGroupId(7)).thenReturn(List.of(schedule));

        EnrollmentDetail detail = service.create(GUARDIAN, new CreateEnrollmentRequest(5, 7, CURRENT));

        ArgumentCaptor<Enrollment> saved = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(saved.capture());
        assertThat(saved.getValue().getAthlete()).isSameAs(athlete);
        assertThat(saved.getValue().getGroup()).isSameAs(group);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(detail.status()).isEqualTo(EnrollmentStatus.PENDING_REVIEW);
        assertThat(detail.paymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
        assertThat(detail.paymentReported()).isFalse();
        assertThat(detail.period()).isEqualTo(CURRENT);
        assertThat(detail.athlete().firstNames()).isEqualTo("Ana");
        assertThat(detail.discipline().name()).isEqualTo("Fútbol");
        assertThat(detail.schedules()).hasSize(1);
        assertThat(detail.confirmedAt()).isNull();
    }

    @Test
    void locksTheAthleteBeforeTheGroupAndChecksSpotsUnderTheLock() {
        athlete(5, TODAY.minusYears(14));
        group(7, 15, EntityStatus.ACTIVE, EntityStatus.ACTIVE);

        service.create(ADMIN, new CreateEnrollmentRequest(5, 7, CURRENT.plusMonths(2)));

        InOrder order = inOrder(athleteRepository, groupRepository, enrollmentRepository);
        order.verify(athleteRepository).findLockedById(5);
        order.verify(groupRepository).findLockedById(7);
        order.verify(enrollmentRepository).existsByAthleteIdAndGroupDisciplineIdAndStatusIn(5, 3,
                EnrollmentStatus.IN_FORCE);
        order.verify(enrollmentRepository).countByGroupIdAndStatusIn(7, EnrollmentStatus.IN_FORCE);
        order.verify(enrollmentRepository).save(any());
    }

    @Test
    void guardianCannotEnrollAnotherGuardiansAthlete() {
        athlete(5, TODAY.minusYears(14));
        group(7, 15, EntityStatus.ACTIVE, EntityStatus.ACTIVE);

        BusinessException ex = failure(() -> service.create(OTHER_GUARDIAN, new CreateEnrollmentRequest(5, 7, CURRENT)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void missingAthleteOrGroupIsNotFound() {
        athlete(5, TODAY.minusYears(14));

        assertThat(failure(() -> service.create(ADMIN, new CreateEnrollmentRequest(9, 7, CURRENT))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.create(ADMIN, new CreateEnrollmentRequest(5, 9, CURRENT))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void athleteMustStillBeBetweenTwelveAndSeventeen() {
        athlete(5, TODAY.minusYears(18));
        group(7, 15, EntityStatus.ACTIVE, EntityStatus.ACTIVE);

        assertThat(failure(() -> service.create(ADMIN, new CreateEnrollmentRequest(5, 7, CURRENT))).getCode())
                .isEqualTo(ErrorCode.AGE_OUT_OF_RANGE);
    }

    @Test
    void periodCannotBeBeforeTheCurrentMonth() {
        athlete(5, TODAY.minusYears(14));
        group(7, 15, EntityStatus.ACTIVE, EntityStatus.ACTIVE);

        BusinessException ex = failure(() -> service.create(ADMIN, new CreateEnrollmentRequest(5, 7,
                CURRENT.minusMonths(1))));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERIOD_IN_PAST);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void groupAndDisciplineMustBeActive() {
        athlete(5, TODAY.minusYears(14));
        group(7, 15, EntityStatus.INACTIVE, EntityStatus.ACTIVE);
        group(8, 15, EntityStatus.ACTIVE, EntityStatus.INACTIVE);

        for (int groupId : new int[]{7, 8}) {
            BusinessException ex = failure(() -> service.create(ADMIN, new CreateEnrollmentRequest(5, groupId, CURRENT)));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.GROUP_INACTIVE);
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    @Test
    void athleteCannotHaveTwoEnrollmentsInForceInTheSameDiscipline() {
        athlete(5, TODAY.minusYears(14));
        group(7, 15, EntityStatus.ACTIVE, EntityStatus.ACTIVE);
        when(enrollmentRepository.existsByAthleteIdAndGroupDisciplineIdAndStatusIn(5, 3, EnrollmentStatus.IN_FORCE))
                .thenReturn(true);

        BusinessException ex = failure(() -> service.create(ADMIN, new CreateEnrollmentRequest(5, 7, CURRENT)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.DUPLICATE_ENROLLMENT);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void fullGroupRejectsTheRequestWithoutCreatingIt() {
        athlete(5, TODAY.minusYears(14));
        group(7, 2, EntityStatus.ACTIVE, EntityStatus.ACTIVE);
        when(enrollmentRepository.countByGroupIdAndStatusIn(7, EnrollmentStatus.IN_FORCE)).thenReturn(2L);

        BusinessException ex = failure(() -> service.create(ADMIN, new CreateEnrollmentRequest(5, 7, CURRENT)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.NO_AVAILABLE_SPOTS);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(enrollmentRepository, never()).save(any());
    }

    private Athlete athlete(int id, LocalDate birthDate) {
        User user = new User(new Role(RoleName.GUARDIAN, null), "g@teensport.test", "$2a$hash", NOW);
        Guardian guardian = new Guardian(user, null);
        ReflectionTestUtils.setField(guardian, "id", 20);
        Athlete athlete = new Athlete(guardian, NOW);
        athlete.setFirstName("Ana");
        athlete.setLastName("Prueba");
        athlete.setBirthDate(birthDate);
        ReflectionTestUtils.setField(athlete, "id", id);
        when(athleteRepository.findLockedById(id)).thenReturn(Optional.of(athlete));
        return athlete;
    }

    private Group group(int id, int capacity, EntityStatus status, EntityStatus disciplineStatus) {
        Discipline discipline = new Discipline("Fútbol", null, BigDecimal.TEN);
        discipline.setStatus(disciplineStatus);
        ReflectionTestUtils.setField(discipline, "id", 3);
        Group group = new Group(discipline, null, "Grupo " + id, capacity);
        group.setStatus(status);
        ReflectionTestUtils.setField(group, "id", id);
        when(groupRepository.findLockedById(id)).thenReturn(Optional.of(group));
        return group;
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
