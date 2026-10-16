package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.EnrollmentSummary;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.EnrollmentFilter;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T17:00:00Z");
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, RoleName.ADMIN, null, null);
    private static final AuthenticatedUser GUARDIAN = new AuthenticatedUser(2, RoleName.GUARDIAN, 20, null);
    private static final AuthenticatedUser OTHER_GUARDIAN = new AuthenticatedUser(3, RoleName.GUARDIAN, 30, null);
    private static final AuthenticatedUser COACH = new AuthenticatedUser(4, RoleName.COACH, null, 10);
    private static final EnrollmentFilter NO_FILTER =
            new EnrollmentFilter(null, null, null, null, null, null, null, null);

    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final EnrollmentQueryService service =
            new EnrollmentQueryService(enrollmentRepository, scheduleRepository, new AthleteAccessPolicy());

    @Test
    void ownerAndAdminSeeTheEnrollmentButAnotherGuardianDoesNot() {
        when(enrollmentRepository.findDetailedById(9)).thenReturn(Optional.of(enrollment(9)));

        assertThat(service.get(GUARDIAN, 9).id()).isEqualTo(9);
        assertThat(service.get(ADMIN, 9).id()).isEqualTo(9);
        BusinessException ex = failure(() -> service.get(OTHER_GUARDIAN, 9));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(scheduleRepository, times(2)).findByGroupId(7);
    }

    @Test
    void missingEnrollmentIsNotFound() {
        assertThat(failure(() -> service.get(ADMIN, 99)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listMapsThePageNewestFirst() {
        Pageable pageable = PageRequest.of(0, 10, EnrollmentQueryService.ORDER);
        when(enrollmentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(enrollment(9)), pageable, 1));

        PageResponse<EnrollmentSummary> page = service.list(GUARDIAN, NO_FILTER, 0, 10);

        assertThat(page.content()).singleElement().satisfies(summary -> {
            assertThat(summary.period()).isEqualTo(YearMonth.of(2026, 11));
            assertThat(summary.group().name()).isEqualTo("Grupo A");
        });
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void coachCannotListEnrollments() {
        assertThat(failure(() -> service.list(COACH, NO_FILTER, 0, 10)).getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    private static Enrollment enrollment(int id) {
        Guardian guardian = new Guardian(new User(new Role(RoleName.GUARDIAN, null), "g@teensport.test", "$2a$hash",
                NOW), null);
        ReflectionTestUtils.setField(guardian, "id", 20);
        Athlete athlete = new Athlete(guardian, NOW);
        athlete.setFirstName("Ana");
        athlete.setLastName("Prueba");
        ReflectionTestUtils.setField(athlete, "id", 5);
        Discipline discipline = new Discipline("Fútbol", null, BigDecimal.TEN);
        ReflectionTestUtils.setField(discipline, "id", 3);
        Group group = new Group(discipline, null, "Grupo A", 15);
        ReflectionTestUtils.setField(group, "id", 7);
        Enrollment enrollment = new Enrollment(athlete, group, YearMonth.of(2026, 11), NOW);
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
