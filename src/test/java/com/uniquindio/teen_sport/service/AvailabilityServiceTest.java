package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.GroupAvailability;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupOccupancy;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvailabilityServiceTest {

    /** 12 de octubre de 2026 a mediodía en Bogotá. */
    private static final Instant NOW = Instant.parse("2026-10-12T17:00:00Z");
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final AvailabilityService service = new AvailabilityService(groupRepository, scheduleRepository,
            new SpotCounter(enrollmentRepository), new AcademyCalendar(Clock.fixed(NOW, ZoneOffset.UTC), "America/Bogota"));

    @Test
    void periodInThePastIsRejectedBeforeQuerying() {
        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.list(YearMonth.of(2026, 9), null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERIOD_IN_PAST);
        verifyNoInteractions(groupRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void occupiedCountsEnrollmentsStartedByThePeriodAndAvailableDiscountsAllInForce() {
        Group busy = group(7, 4);
        Group empty = group(8, 10);
        when(groupRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(busy, empty));
        when(enrollmentRepository.countOccupancy(List.of(7, 8), "2026-11", EnrollmentStatus.IN_FORCE))
                .thenReturn(List.of(new Row(7, 1L, 3L)));

        List<GroupAvailability> availability = service.list(NOVEMBER, null);

        assertThat(availability).extracting(GroupAvailability::groupId).containsExactly(7, 8);
        assertThat(availability.get(0).occupiedSpots()).isEqualTo(1);
        assertThat(availability.get(0).availableSpots()).isEqualTo(1);
        assertThat(availability.get(0).discipline().monthlyFee()).isEqualByComparingTo("120000");
        assertThat(availability.get(0).coach().fullName()).isEqualTo("Luis Pérez");
        assertThat(availability.get(1).occupiedSpots()).isZero();
        assertThat(availability.get(1).availableSpots()).isEqualTo(10);
    }

    @Test
    @SuppressWarnings("unchecked")
    void noOpenGroupsSkipsTheOtherQueries() {
        when(groupRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        assertThat(service.list(NOVEMBER, 5)).isEmpty();
        verify(scheduleRepository, never()).findByGroupIds(anyCollection());
        verifyNoInteractions(enrollmentRepository);
    }

    private static Group group(int id, int capacity) {
        User user = new User(new Role(RoleName.COACH, null), "c@teensport.test", "$2a$hash", NOW);
        user.setFirstName("Luis");
        user.setLastName("Pérez");
        Coach coach = new Coach(user, null, null);
        ReflectionTestUtils.setField(coach, "id", 10);
        Discipline discipline = new Discipline("Fútbol", null, new BigDecimal("120000"));
        ReflectionTestUtils.setField(discipline, "id", 3);
        Group group = new Group(discipline, coach, "Grupo " + id, capacity);
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private record Row(Integer getGroupId, Long getStartedBy, Long getInForce) implements GroupOccupancy {
    }
}
