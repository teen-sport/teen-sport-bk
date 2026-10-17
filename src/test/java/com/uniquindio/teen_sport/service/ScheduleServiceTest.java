package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.ScheduleRequest;
import com.uniquindio.teen_sport.dto.ScheduleResponse;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final CoachRepository coachRepository = mock(CoachRepository.class);
    private final ScheduleService service = new ScheduleService(scheduleRepository, groupRepository, coachRepository,
            new ScheduleConflictChecker(scheduleRepository));
    private Group group;

    @BeforeEach
    void setUp() {
        Coach coach = new Coach(null, null, null);
        ReflectionTestUtils.setField(coach, "id", 10);
        group = new Group(new Discipline("Fútbol", null, BigDecimal.TEN), coach, "A", 15);
        ReflectionTestUtils.setField(group, "id", 7);
        when(groupRepository.findLockedById(7)).thenReturn(Optional.of(group));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsScheduleLockingGroupAndCoach() {
        ScheduleResponse created = service.create(7, request(DayOfWeek.MONDAY, "16:00", "18:00"));

        assertThat(created.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(created.startTime()).isEqualTo(LocalTime.of(16, 0));
        verify(groupRepository).findLockedById(7);
        verify(coachRepository).findLockedById(10);
    }

    @Test
    void endMustBeAfterStart() {
        BusinessException ex = failure(() -> service.create(7, request(DayOfWeek.MONDAY, "18:00", "18:00")));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_TIME_RANGE);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void rejectsOverlapWithTheGroupOrWithAnotherActiveGroupOfTheCoach() {
        when(scheduleRepository.findByGroupId(7)).thenReturn(List.of(existing(70, group, "16:00", "18:00")));
        Group other = new Group(group.getDiscipline(), group.getCoach(), "B", 15);
        ReflectionTestUtils.setField(other, "id", 8);
        when(scheduleRepository.findActiveByCoachExcludingGroup(10, 7))
                .thenReturn(List.of(existing(80, other, "19:00", "20:00")));

        BusinessException sameGroup = failure(() -> service.create(7, request(DayOfWeek.MONDAY, "17:00", "19:00")));
        BusinessException coachGroup = failure(() -> service.create(7, request(DayOfWeek.MONDAY, "19:30", "21:00")));

        assertThat(sameGroup.getCode()).isEqualTo(ErrorCode.SCHEDULE_CONFLICT);
        assertThat(sameGroup.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(coachGroup.getCode()).isEqualTo(ErrorCode.SCHEDULE_CONFLICT);
        assertThat(service.create(7, request(DayOfWeek.MONDAY, "18:00", "19:00")).endTime())
                .isEqualTo(LocalTime.of(19, 0));
    }

    @Test
    void updateIgnoresTheScheduleBeingEdited() {
        Schedule schedule = existing(70, group, "16:00", "18:00");
        when(scheduleRepository.findWithGroupById(70)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.findByGroupId(7)).thenReturn(List.of(schedule));

        ScheduleResponse updated = service.update(70, request(DayOfWeek.MONDAY, "16:30", "18:30"));

        assertThat(updated.startTime()).isEqualTo(LocalTime.of(16, 30));
        assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(18, 30));
    }

    @Test
    void missingGroupOrScheduleIsNotFound() {
        assertThat(failure(() -> service.create(99, request(DayOfWeek.MONDAY, "16:00", "18:00"))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.update(99, request(DayOfWeek.MONDAY, "16:00", "18:00"))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.delete(99)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletesSchedule() {
        Schedule schedule = existing(70, group, "16:00", "18:00");
        when(scheduleRepository.findWithGroupById(70)).thenReturn(Optional.of(schedule));

        service.delete(70);

        verify(scheduleRepository).delete(schedule);
    }

    private static Schedule existing(int id, Group group, String start, String end) {
        Schedule schedule = new Schedule(group);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.parse(start));
        schedule.setEndTime(LocalTime.parse(end));
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private static ScheduleRequest request(DayOfWeek day, String start, String end) {
        return new ScheduleRequest(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
