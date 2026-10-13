package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CreateGroupRequest;
import com.uniquindio.teen_sport.dto.GroupResponse;
import com.uniquindio.teen_sport.dto.UpdateGroupRequest;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.DisciplineRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceTest {

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, RoleName.ADMIN, null, null);
    private static final AuthenticatedUser COACH = new AuthenticatedUser(2, RoleName.COACH, null, 10);
    private static final AuthenticatedUser OTHER_COACH = new AuthenticatedUser(3, RoleName.COACH, null, 11);

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final DisciplineRepository disciplineRepository = mock(DisciplineRepository.class);
    private final CoachRepository coachRepository = mock(CoachRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private GroupService service;

    @BeforeEach
    void setUp() {
        service = new GroupService(groupRepository, scheduleRepository, disciplineRepository, coachRepository,
                enrollmentRepository, new GroupAccessPolicy(), new ScheduleConflictChecker(scheduleRepository));
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsActiveGroupWithoutSchedules() {
        discipline(5, EntityStatus.ACTIVE);
        coach(10, EntityStatus.ACTIVE);

        GroupResponse group = service.create(new CreateGroupRequest(5, 10, " Sub-15 A ", 15));

        assertThat(group.name()).isEqualTo("Sub-15 A");
        assertThat(group.status()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(group.discipline().name()).isEqualTo("Fútbol");
        assertThat(group.coach().id()).isEqualTo(10);
        assertThat(group.coach().fullName()).isEqualTo("Luis Pérez");
        assertThat(group.schedules()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 16, -1})
    void capacityMustBeBetweenOneAndFifteen(int capacity) {
        BusinessException ex = failure(() -> service.create(new CreateGroupRequest(5, 10, "A", capacity)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_MAX_CAPACITY);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        verify(groupRepository, never()).save(any());
    }

    @Test
    void creationRequiresExistingActiveDisciplineAndCoach() {
        discipline(6, EntityStatus.INACTIVE);
        discipline(5, EntityStatus.ACTIVE);
        coach(12, EntityStatus.INACTIVE);

        assertThat(failure(() -> service.create(new CreateGroupRequest(99, 10, "A", 10))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.create(new CreateGroupRequest(6, 10, "A", 10))).getCode())
                .isEqualTo(ErrorCode.DISCIPLINE_INACTIVE);
        assertThat(failure(() -> service.create(new CreateGroupRequest(5, 99, "A", 10))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(failure(() -> service.create(new CreateGroupRequest(5, 12, "A", 10))).getCode())
                .isEqualTo(ErrorCode.INVALID_COACH);
    }

    @Test
    void coachOnlyAccessesOwnGroups() {
        group(7, coach(10, EntityStatus.ACTIVE));

        assertThat(service.get(COACH, 7).id()).isEqualTo(7);
        assertThat(service.get(ADMIN, 7).id()).isEqualTo(7);
        BusinessException ex = failure(() -> service.get(OTHER_COACH, 7));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listLoadsAllSchedulesInOneQuery() {
        Group first = group(7, coach(10, EntityStatus.ACTIVE));
        Group second = group(8, coach(10, EntityStatus.ACTIVE));
        Schedule schedule = schedule(70, first, DayOfWeek.MONDAY, "16:00", "18:00");
        when(groupRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(first, second));
        when(scheduleRepository.findByGroupIds(anyCollection())).thenReturn(List.of(schedule));

        List<GroupResponse> groups = service.list(COACH, null, 99, null);

        assertThat(groups).extracting(GroupResponse::id).containsExactly(7, 8);
        assertThat(groups.get(0).schedules()).hasSize(1);
        assertThat(groups.get(1).schedules()).isEmpty();
        verify(scheduleRepository, never()).findByGroupId(any());
    }

    @Test
    void updateValidatesCapacityAndChangesData() {
        Group group = group(7, coach(10, EntityStatus.ACTIVE));

        assertThat(failure(() -> service.update(7, new UpdateGroupRequest("A", 20))).getCode())
                .isEqualTo(ErrorCode.INVALID_MAX_CAPACITY);
        service.update(7, new UpdateGroupRequest(" Nuevo ", 12));

        assertThat(group.getName()).isEqualTo("Nuevo");
        assertThat(group.getMaxCapacity()).isEqualTo(12);
        assertThat(failure(() -> service.update(99, new UpdateGroupRequest("A", 5))).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void capacityCannotDropBelowEnrollmentsInForce() {
        Group group = group(7, coach(10, EntityStatus.ACTIVE));
        when(enrollmentRepository.countByGroupIdAndStatusIn(7, EnrollmentStatus.IN_FORCE)).thenReturn(5L);

        BusinessException ex = failure(() -> service.update(7, new UpdateGroupRequest("A", 4)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.CAPACITY_BELOW_OCCUPIED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(group.getMaxCapacity()).isEqualTo(15);
        verify(groupRepository).findLockedById(7);

        service.update(7, new UpdateGroupRequest("A", 5));
        assertThat(group.getMaxCapacity()).isEqualTo(5);
    }

    @Test
    void groupWithEnrollmentsInForceCannotBeDeactivated() {
        Group group = group(7, coach(10, EntityStatus.ACTIVE));
        when(enrollmentRepository.existsByGroupIdAndStatusIn(7, EnrollmentStatus.IN_FORCE)).thenReturn(true);

        BusinessException ex = failure(() -> service.changeStatus(7, EntityStatus.INACTIVE));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.GROUP_HAS_ACTIVITY);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(group.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        verify(groupRepository).findLockedById(7);
    }

    @Test
    void keepingTheSameStatusChangesNothing() {
        group(7, coach(10, EntityStatus.ACTIVE));

        assertThat(service.changeStatus(7, EntityStatus.ACTIVE).status()).isEqualTo(EntityStatus.ACTIVE);

        verify(groupRepository, never()).findLockedById(any());
        verify(enrollmentRepository, never()).existsByGroupIdAndStatusIn(any(), any());
    }

    @Test
    void reactivatingGroupChecksCoachConflictsWithLocks() {
        Coach coach = coach(10, EntityStatus.ACTIVE);
        Group group = group(7, coach);
        group.setStatus(EntityStatus.INACTIVE);
        Schedule own = schedule(70, group, DayOfWeek.MONDAY, "16:00", "18:00");
        Schedule other = schedule(80, group(8, coach), DayOfWeek.MONDAY, "17:00", "19:00");
        when(scheduleRepository.findByGroupId(7)).thenReturn(List.of(own));
        when(scheduleRepository.findActiveByCoachExcludingGroup(10, 7)).thenReturn(List.of(other));

        BusinessException ex = failure(() -> service.changeStatus(7, EntityStatus.ACTIVE));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.COACH_SCHEDULE_CONFLICT);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(group.getStatus()).isEqualTo(EntityStatus.INACTIVE);
        verify(groupRepository).findLockedById(7);
        verify(coachRepository).findLockedById(10);
    }

    @Test
    void deactivatingOrKeepingActiveSkipsConflictCheck() {
        group(7, coach(10, EntityStatus.ACTIVE));

        service.changeStatus(7, EntityStatus.ACTIVE);
        service.changeStatus(7, EntityStatus.INACTIVE);

        verify(scheduleRepository, never()).findActiveByCoachExcludingGroup(any(), any());
    }

    @Test
    void changesStatus() {
        Group group = group(7, coach(10, EntityStatus.ACTIVE));

        assertThat(service.changeStatus(7, EntityStatus.INACTIVE).status()).isEqualTo(EntityStatus.INACTIVE);
        assertThat(group.getStatus()).isEqualTo(EntityStatus.INACTIVE);
    }

    @Test
    void assignCoachRequiresActiveCoachWithoutConflicts() {
        Group group = group(7, coach(10, EntityStatus.ACTIVE));
        when(groupRepository.findLockedById(7)).thenReturn(Optional.of(group));
        Coach busy = coach(11, EntityStatus.ACTIVE);
        Coach free = coach(13, EntityStatus.ACTIVE);
        coach(12, EntityStatus.INACTIVE);
        Schedule own = schedule(70, group, DayOfWeek.MONDAY, "16:00", "18:00");
        Schedule otherGroup = schedule(80, group(8, busy), DayOfWeek.MONDAY, "17:00", "19:00");
        when(scheduleRepository.findByGroupId(7)).thenReturn(List.of(own));
        when(scheduleRepository.findActiveByCoachExcludingGroup(11, 7)).thenReturn(List.of(otherGroup));

        assertThat(failure(() -> service.assignCoach(7, 12)).getCode()).isEqualTo(ErrorCode.INVALID_COACH);
        BusinessException conflict = failure(() -> service.assignCoach(7, 11));
        assertThat(conflict.getCode()).isEqualTo(ErrorCode.COACH_SCHEDULE_CONFLICT);
        assertThat(conflict.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(group.getCoach().getId()).isEqualTo(10);

        assertThat(service.assignCoach(7, 13).coach().id()).isEqualTo(13);
        assertThat(group.getCoach()).isSameAs(free);
        verify(coachRepository).findLockedById(13);
    }

    @Test
    void assigningTheSameCoachSkipsConflictCheck() {
        Group group = group(7, coach(10, EntityStatus.ACTIVE));
        when(groupRepository.findLockedById(7)).thenReturn(Optional.of(group));

        service.assignCoach(7, 10);

        verify(scheduleRepository, never()).findActiveByCoachExcludingGroup(any(), any());
    }

    private Discipline discipline(int id, EntityStatus status) {
        Discipline discipline = new Discipline("Fútbol", null, BigDecimal.TEN);
        discipline.setStatus(status);
        ReflectionTestUtils.setField(discipline, "id", id);
        when(disciplineRepository.findById(id)).thenReturn(Optional.of(discipline));
        return discipline;
    }

    private Coach coach(int id, EntityStatus status) {
        User user = new User(new Role(RoleName.COACH, null), "c" + id + "@teensport.test", "$2a$hash", Instant.now());
        user.setFirstName("Luis");
        user.setLastName("Pérez");
        user.setStatus(status);
        Coach coach = new Coach(user, null, null);
        ReflectionTestUtils.setField(coach, "id", id);
        when(coachRepository.findWithUserById(id)).thenReturn(Optional.of(coach));
        return coach;
    }

    private Group group(int id, Coach coach) {
        Discipline discipline = new Discipline("Fútbol", null, BigDecimal.TEN);
        ReflectionTestUtils.setField(discipline, "id", 5);
        Group group = new Group(discipline, coach, "Grupo " + id, 15);
        ReflectionTestUtils.setField(group, "id", id);
        when(groupRepository.findDetailedById(id)).thenReturn(Optional.of(group));
        return group;
    }

    private static Schedule schedule(int id, Group group, DayOfWeek day, String start, String end) {
        Schedule schedule = new Schedule(group);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.parse(start));
        schedule.setEndTime(LocalTime.parse(end));
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
