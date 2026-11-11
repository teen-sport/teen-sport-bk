package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.AthleteAttendanceReport;
import com.uniquindio.teen_sport.dto.AttendanceReport;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.AthleteRepository;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupMarkCount;
import com.uniquindio.teen_sport.repository.GroupSessionCount;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.uniquindio.teen_sport.service.SessionFixtures.ADMIN;
import static com.uniquindio.teen_sport.service.SessionFixtures.COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.GUARDIAN;
import static com.uniquindio.teen_sport.service.SessionFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R2 y R3: indicadores de asistencia y asistencia de un deportista.
 */
class AttendanceReportServiceTest {

    private static final int ATHLETE_ID = 30;

    private final TrainingSessionRepository sessionRepository = mock(TrainingSessionRepository.class);
    private final AttendanceRecordRepository attendanceRepository = mock(AttendanceRecordRepository.class);
    private final AthleteRepository athleteRepository = mock(AthleteRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private AttendanceReportService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceReportService(sessionRepository, attendanceRepository, athleteRepository,
                enrollmentRepository, new GroupAccessPolicy(), new AthleteAccessPolicy(), SessionFixtures.calendar());
        Athlete athlete = SessionFixtures.athlete(ATHLETE_ID, "Ana", "Pérez");
        when(athleteRepository.findById(ATHLETE_ID)).thenReturn(Optional.of(athlete));
    }

    @Test
    void attendanceReportDefaultsToTheCurrentMonthAndComputesRates() {
        LocalDate monthStart = TODAY.withDayOfMonth(1);
        List<GroupSessionCount> sessions = List.of(sessions(7, "Sub-14", "Fútbol", 8, 6),
                sessions(9, "Sub-16", "Voleibol", 2, 0));
        List<GroupMarkCount> marks = List.of(marks(7, "Sub-14", 25, 5));
        when(sessionRepository.countSessionsByGroup(monthStart, TODAY, null, null, null)).thenReturn(sessions);
        when(attendanceRepository.countMarksByGroup(monthStart, TODAY, null, null, null)).thenReturn(marks);

        AttendanceReport report = service.attendanceReport(ADMIN, null, null, null, null);

        assertThat(report.scheduledSessions()).isEqualTo(10);
        assertThat(report.completedSessions()).isEqualTo(6);
        assertThat(report.recordingCoverage()).isEqualTo(60.0);
        assertThat(report.attendanceRate()).isEqualTo(83.3);
        assertThat(report.byGroup()).containsExactly(
                new AttendanceReport.GroupAttendance(7, "Sub-14", "Fútbol", 8, 6, 83.3),
                new AttendanceReport.GroupAttendance(9, "Sub-16", "Voleibol", 2, 0, null));
    }

    @Test
    void attendanceReportOnlyCountsSessionsUpToTodayAndLimitsCoachesToTheirGroups() {
        LocalDate from = TODAY.minusDays(5);

        AttendanceReport report = service.attendanceReport(COACH, from, TODAY.plusDays(20), 7, 3);

        verify(sessionRepository).countSessionsByGroup(from, TODAY, 7, 3, COACH.coachId());
        verify(attendanceRepository).countMarksByGroup(from, TODAY, 7, 3, COACH.coachId());
        assertThat(report.scheduledSessions()).isZero();
        assertThat(report.recordingCoverage()).isZero();
        assertThat(report.attendanceRate()).isNull();
        assertThat(report.byGroup()).isEmpty();
    }

    @Test
    void attendanceReportRejectsReversedOrTooLongRanges() {
        assertInvalidRange(() -> service.attendanceReport(ADMIN, TODAY, TODAY.minusDays(1), null, null));
        assertInvalidRange(() -> service.attendanceReport(ADMIN, TODAY.minusDays(366), TODAY, null, null));
        verify(sessionRepository, never()).countSessionsByGroup(any(), any(), any(), any(), any());
    }

    @Test
    void attendanceReportAcceptsAFullYear() {
        service.attendanceReport(ADMIN, TODAY.minusDays(365), TODAY, null, null);

        verify(sessionRepository).countSessionsByGroup(TODAY.minusDays(365), TODAY, null, null, null);
    }

    @Test
    void attendanceReportIsDeniedToGuardians() {
        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.attendanceReport(GUARDIAN, null, null, null, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void athleteReportForAdminCoversTheWholeHistoryOfAllGroups() {
        List<GroupMarkCount> marks = List.of(marks(7, "Sub-14", 3, 1), marks(9, "Sub-16", 0, 0));
        when(attendanceRepository.countAthleteMarksByGroup(ATHLETE_ID, null, null, null)).thenReturn(marks);

        AthleteAttendanceReport report = service.athleteReport(ADMIN, ATHLETE_ID, null, null);

        assertThat(report.athleteId()).isEqualTo(ATHLETE_ID);
        assertThat(report.recordedSessions()).isEqualTo(4);
        assertThat(report.present()).isEqualTo(3);
        assertThat(report.absent()).isEqualTo(1);
        assertThat(report.attendanceRate()).isEqualTo(75.0);
        assertThat(report.byGroup()).containsExactly(
                new AthleteAttendanceReport.GroupMarks(7, "Sub-14", 3, 1, 75.0),
                new AthleteAttendanceReport.GroupMarks(9, "Sub-16", 0, 0, null));
    }

    @Test
    void athleteReportWithoutMarksHasNoRate() {
        AthleteAttendanceReport report = service.athleteReport(GUARDIAN, ATHLETE_ID, TODAY.minusDays(3), TODAY);

        verify(attendanceRepository).countAthleteMarksByGroup(ATHLETE_ID, TODAY.minusDays(3), TODAY, null);
        assertThat(report.recordedSessions()).isZero();
        assertThat(report.attendanceRate()).isNull();
        assertThat(report.byGroup()).isEmpty();
    }

    @Test
    void athleteReportOfAnotherGuardiansAthleteIsDenied() {
        AuthenticatedUser otherGuardian = new AuthenticatedUser(5, GUARDIAN.role(), 99, null);

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.athleteReport(otherGuardian, ATHLETE_ID, null, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(attendanceRepository, never()).countAthleteMarksByGroup(anyInt(), any(), any(), any());
    }

    @Test
    void coachSeesOnlyTheirGroupsMarksOfAnAthleteEnrolledWithThem() {
        when(enrollmentRepository.existsByAthleteIdAndGroupCoachIdAndStatusIn(ATHLETE_ID, COACH.coachId(),
                EnrollmentStatus.IN_FORCE)).thenReturn(true);

        service.athleteReport(COACH, ATHLETE_ID, null, null);

        verify(attendanceRepository).countAthleteMarksByGroup(eq(ATHLETE_ID), isNull(), isNull(), eq(COACH.coachId()));
    }

    @Test
    void coachCannotSeeAnAthleteWithoutEnrollmentInTheirGroups() {
        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.athleteReport(COACH, ATHLETE_ID, null, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void athleteReportOfAMissingAthleteIsNotFound() {
        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.athleteReport(ADMIN, 404, null, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(AttendanceReportService.ATHLETE_NOT_FOUND_MESSAGE);
    }

    @Test
    void athleteReportRejectsReversedRangesBeforeLookingUpTheAthlete() {
        assertInvalidRange(() -> service.athleteReport(ADMIN, ATHLETE_ID, TODAY, TODAY.minusDays(1)));
        verify(athleteRepository, never()).findById(any());
    }

    private static void assertInvalidRange(Runnable call) {
        BusinessException ex = catchThrowableOfType(BusinessException.class, call::run);
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    private static GroupSessionCount sessions(int groupId, String name, String discipline, long scheduled,
                                              long completed) {
        GroupSessionCount count = mock(GroupSessionCount.class);
        when(count.getGroupId()).thenReturn(groupId);
        when(count.getGroupName()).thenReturn(name);
        when(count.getDiscipline()).thenReturn(discipline);
        when(count.getScheduled()).thenReturn(scheduled);
        when(count.getCompleted()).thenReturn(completed);
        return count;
    }

    private static GroupMarkCount marks(int groupId, String name, long present, long absent) {
        GroupMarkCount count = mock(GroupMarkCount.class);
        when(count.getGroupId()).thenReturn(groupId);
        when(count.getGroupName()).thenReturn(name);
        when(count.getPresent()).thenReturn(present);
        when(count.getAbsent()).thenReturn(absent);
        return count;
    }
}
