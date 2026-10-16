package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CreateSessionRequest;
import com.uniquindio.teen_sport.dto.GenerateSessionsRequest;
import com.uniquindio.teen_sport.dto.GenerateSessionsResponse;
import com.uniquindio.teen_sport.dto.SessionResponse;
import com.uniquindio.teen_sport.dto.UpdateSessionRequest;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import static com.uniquindio.teen_sport.service.SessionFixtures.ADMIN;
import static com.uniquindio.teen_sport.service.SessionFixtures.COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.GUARDIAN;
import static com.uniquindio.teen_sport.service.SessionFixtures.OTHER_COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private static final LocalTime FOUR = LocalTime.of(16, 0);
    private static final LocalTime FIVE = LocalTime.of(17, 0);

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final TrainingSessionRepository sessionRepository = mock(TrainingSessionRepository.class);
    private SessionService service;
    private Group group;

    @BeforeEach
    void setUp() {
        service = new SessionService(groupRepository, scheduleRepository, sessionRepository, new GroupAccessPolicy(),
                new SessionSlotPolicy(sessionRepository, SessionFixtures.calendar()));
        group = SessionFixtures.group();
        when(groupRepository.findDetailedById(7)).thenReturn(Optional.of(group));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAScheduledSessionForTheGroupCoachUnderTheGroupLock() {
        SessionResponse session = service.create(COACH, new CreateSessionRequest(7, TODAY, FOUR, FIVE, "  Extra  "));

        assertThat(session.status()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(session.attendanceRecorded()).isFalse();
        assertThat(session.coach().id()).isEqualTo(10);
        assertThat(session.coach().fullName()).isEqualTo("Luis Rojas");
        assertThat(session.discipline().id()).isEqualTo(3);
        assertThat(session.notes()).isEqualTo("Extra");
        assertThat(session.athletes()).isNull();
        InOrder order = inOrder(groupRepository, sessionRepository);
        order.verify(groupRepository).findLockedById(7);
        order.verify(sessionRepository).existsByGroupIdAndSessionDateAndStartTime(7, TODAY, FOUR);
    }

    @Test
    void creatingValidatesOwnershipGroupTimesDateAndDuplicates() {
        assertThat(failure(() -> service.create(OTHER_COACH, request(TODAY, FOUR, FIVE))).getCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(failure(() -> service.create(GUARDIAN, request(TODAY, FOUR, FIVE))).getCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(failure(() -> service.create(ADMIN, request(TODAY, FIVE, FOUR))).getCode())
                .isEqualTo(ErrorCode.INVALID_TIME_RANGE);
        assertThat(failure(() -> service.create(ADMIN, request(TODAY.minusDays(1), FOUR, FIVE))).getCode())
                .isEqualTo(ErrorCode.DATE_IN_PAST);
        when(sessionRepository.existsByGroupIdAndSessionDateAndStartTime(7, TODAY, FOUR)).thenReturn(true);
        BusinessException duplicate = failure(() -> service.create(ADMIN, request(TODAY, FOUR, FIVE)));
        assertThat(duplicate.getCode()).isEqualTo(ErrorCode.DUPLICATE_SESSION);
        assertThat(duplicate.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        group.setStatus(EntityStatus.INACTIVE);
        assertThat(failure(() -> service.create(ADMIN, request(TODAY, FOUR, FIVE))).getCode())
                .isEqualTo(ErrorCode.GROUP_INACTIVE);
        assertThat(failure(() -> service.create(ADMIN, new CreateSessionRequest(8, TODAY, FOUR, FIVE, null)))
                .getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void generatesOneSessionPerSlotAndDateSkippingExistingOnes() {
        LocalDate monday = LocalDate.of(2026, 10, 12);
        Schedule late = schedule(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(19, 0));
        Schedule early = schedule(DayOfWeek.MONDAY, FOUR, FIVE);
        Schedule wednesday = schedule(DayOfWeek.WEDNESDAY, FOUR, FIVE);
        when(scheduleRepository.findByGroupId(7)).thenReturn(List.of(late, wednesday, early));
        LocalDate to = monday.plusDays(7);
        when(sessionRepository.findByGroupIdAndSessionDateBetween(7, monday, to))
                .thenReturn(List.of(SessionFixtures.session(1, group, monday)));

        GenerateSessionsResponse result = service.generate(ADMIN, new GenerateSessionsRequest(7, monday, to, true));

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(4);
        assertThat(result.sessions()).extracting(SessionResponse::date, SessionResponse::startTime).containsExactly(
                tuple(monday, LocalTime.of(18, 0)),
                tuple(monday.plusDays(2), FOUR),
                tuple(to, FOUR),
                tuple(to, LocalTime.of(18, 0)));
        verify(groupRepository).findLockedById(7);
    }

    @Test
    void generatingValidatesTheRangeTheDateAndTheSchedules() {
        assertThat(failure(() -> service.generate(ADMIN, generate(TODAY.plusDays(1), TODAY))).getCode())
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        assertThat(failure(() -> service.generate(ADMIN, generate(TODAY, TODAY.plusDays(62)))).getCode())
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        assertThat(failure(() -> service.generate(ADMIN, generate(TODAY.minusDays(1), TODAY))).getCode())
                .isEqualTo(ErrorCode.DATE_IN_PAST);
        BusinessException empty = failure(() -> service.generate(ADMIN, generate(TODAY, TODAY.plusDays(61))));
        assertThat(empty.getCode()).isEqualTo(ErrorCode.GROUP_WITHOUT_SCHEDULES);
        assertThat(empty.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(failure(() -> service.generate(OTHER_COACH, generate(TODAY, TODAY))).getCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(groupRepository, never()).findLockedById(any());
    }

    @Test
    void reschedulingLocksTheGroupThenTheSessionAndIgnoresItself() {
        TrainingSession session = SessionFixtures.session(5, group, TODAY);
        session.setNotes("Antes");
        lockable(session);
        LocalDate tomorrow = TODAY.plusDays(1);

        SessionResponse updated = service.reschedule(COACH, 5,
                new UpdateSessionRequest(tomorrow, LocalTime.of(8, 0), LocalTime.of(9, 0), null));

        assertThat(updated.date()).isEqualTo(tomorrow);
        assertThat(updated.endTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(updated.notes()).isNull();
        InOrder order = inOrder(groupRepository, sessionRepository);
        order.verify(groupRepository).findLockedById(7);
        order.verify(sessionRepository).findLockedById(5);
        order.verify(sessionRepository).existsByGroupIdAndSessionDateAndStartTimeAndIdNot(7, tomorrow,
                LocalTime.of(8, 0), 5);
    }

    @Test
    void onlyScheduledSessionsCanBeRescheduledOrCancelled() {
        TrainingSession session = SessionFixtures.session(5, group, TODAY);
        lockable(session);
        UpdateSessionRequest change = new UpdateSessionRequest(TODAY, FOUR, FIVE, null);
        when(sessionRepository.existsByGroupIdAndSessionDateAndStartTimeAndIdNot(7, TODAY, FOUR, 5)).thenReturn(true);
        assertThat(failure(() -> service.reschedule(ADMIN, 5, change)).getCode())
                .isEqualTo(ErrorCode.DUPLICATE_SESSION);
        assertThat(failure(() -> service.cancel(OTHER_COACH, 5, "Lluvia")).getCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        SessionResponse cancelled = service.cancel(COACH, 5, "  Lluvia ");

        assertThat(cancelled.status()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(cancelled.notes()).isEqualTo("Lluvia");
        BusinessException again = failure(() -> service.cancel(ADMIN, 5, "Otra"));
        assertThat(again.getCode()).isEqualTo(ErrorCode.INVALID_STATE);
        assertThat(again.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(failure(() -> service.reschedule(ADMIN, 5, change)).getCode()).isEqualTo(ErrorCode.INVALID_STATE);
        session.setStatus(SessionStatus.COMPLETED);
        assertThat(failure(() -> service.cancel(ADMIN, 5, "Otra")).getCode()).isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void missingSessionIsNotFound() {
        when(sessionRepository.findGroupIdById(9)).thenReturn(Optional.empty());
        when(sessionRepository.findLockedById(9)).thenReturn(Optional.empty());

        assertThat(failure(() -> service.reschedule(ADMIN, 9, new UpdateSessionRequest(TODAY, FOUR, FIVE, null)))
                .getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.cancel(ADMIN, 9, "X")).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void lockable(TrainingSession session) {
        when(sessionRepository.findGroupIdById(session.getId())).thenReturn(Optional.of(7));
        when(sessionRepository.findLockedById(session.getId())).thenReturn(Optional.of(session));
    }

    private Schedule schedule(DayOfWeek day, LocalTime start, LocalTime end) {
        Schedule schedule = new Schedule(group);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        return schedule;
    }

    private static CreateSessionRequest request(LocalDate date, LocalTime start, LocalTime end) {
        return new CreateSessionRequest(7, date, start, end, null);
    }

    private static GenerateSessionsRequest generate(LocalDate from, LocalDate to) {
        return new GenerateSessionsRequest(7, from, to, null);
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
