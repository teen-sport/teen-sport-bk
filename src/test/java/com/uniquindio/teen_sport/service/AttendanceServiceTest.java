package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.AttendanceMark;
import com.uniquindio.teen_sport.dto.AttendanceRoster;
import com.uniquindio.teen_sport.dto.AttendanceRosterEntry;
import com.uniquindio.teen_sport.dto.RecordAttendanceRequest;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.AttendanceRecord;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.FieldValidationException;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import static com.uniquindio.teen_sport.service.SessionFixtures.ADMIN;
import static com.uniquindio.teen_sport.service.SessionFixtures.COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.GUARDIAN;
import static com.uniquindio.teen_sport.service.SessionFixtures.NOW;
import static com.uniquindio.teen_sport.service.SessionFixtures.OTHER_COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceServiceTest {

    private final TrainingSessionRepository sessionRepository = mock(TrainingSessionRepository.class);
    private final AttendanceRecordRepository attendanceRepository = mock(AttendanceRecordRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final AttendanceService service = new AttendanceService(sessionRepository, attendanceRepository,
            enrollmentRepository, new GroupAccessPolicy(), SessionFixtures.calendar());
    private final Athlete sofia = SessionFixtures.athlete(5, "Sofía", "Álvarez");
    private final Athlete luis = SessionFixtures.athlete(6, "Luis", "Zea");
    private Group group;
    private TrainingSession session;

    @BeforeEach
    void setUp() {
        group = SessionFixtures.group();
        session = SessionFixtures.session(1, group, TODAY);
        when(sessionRepository.findDetailedById(1)).thenReturn(Optional.of(session));
        when(sessionRepository.findLockedById(1)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findRosterAthletes(7, "2026-10")).thenReturn(List.of(sofia, luis));
    }

    @Test
    void rosterShowsSavedMarksAndNullForTheRest() {
        when(attendanceRepository.findBySessionId(1))
                .thenReturn(List.of(new AttendanceRecord(session, luis, AttendanceStatus.ABSENT, NOW)));

        AttendanceRoster roster = service.roster(ADMIN, 1);

        assertThat(roster.group().id()).isEqualTo(7);
        assertThat(roster.attendanceRecorded()).isFalse();
        assertThat(roster.records()).extracting(AttendanceRosterEntry::athleteId, AttendanceRosterEntry::status)
                .containsExactly(tuple(5, null),
                        tuple(6, AttendanceStatus.ABSENT));
        assertThat(service.roster(COACH, 1).records()).hasSize(2);
        assertThat(failure(() -> service.roster(OTHER_COACH, 1)).getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(failure(() -> service.roster(GUARDIAN, 1)).getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(failure(() -> service.roster(ADMIN, 2)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordingCreatesMissingMarksUpdatesSavedOnesAndCompletesTheSession() {
        Instant earlier = NOW.minusSeconds(3600);
        AttendanceRecord saved = new AttendanceRecord(session, luis, AttendanceStatus.PRESENT, earlier);
        when(attendanceRepository.findBySessionId(1)).thenReturn(List.of(saved));

        AttendanceRoster roster = service.recordAttendance(COACH, 1,
                request(5, AttendanceStatus.PRESENT, 6, AttendanceStatus.ABSENT));

        assertThat(roster.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(roster.attendanceRecorded()).isTrue();
        assertThat(roster.records()).extracting(AttendanceRosterEntry::status)
                .containsExactly(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT);
        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(saved.getCreatedAt()).isEqualTo(earlier);
        assertThat(saved.getRecordedAt()).isEqualTo(NOW);
        ArgumentCaptor<List<AttendanceRecord>> created = ArgumentCaptor.forClass(List.class);
        verify(attendanceRepository).saveAll(created.capture());
        assertThat(created.getValue()).singleElement().satisfies(attendance -> {
            assertThat(attendance.getAthlete()).isSameAs(sofia);
            assertThat(attendance.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void onlyTheGroupCoachRecordsAndOnlyForPastOrTodaysOpenSessions() {
        RecordAttendanceRequest full = request(5, AttendanceStatus.PRESENT, 6, AttendanceStatus.ABSENT);
        assertThat(failure(() -> service.recordAttendance(ADMIN, 1, full)).getCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(failure(() -> service.recordAttendance(OTHER_COACH, 1, full)).getCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        session.reschedule(TODAY.plusDays(1), session.getStartTime(), session.getEndTime());
        BusinessException future = failure(() -> service.recordAttendance(COACH, 1, full));
        assertThat(future.getCode()).isEqualTo(ErrorCode.FUTURE_SESSION);
        assertThat(future.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);

        session.setStatus(SessionStatus.CANCELLED);
        BusinessException cancelled = failure(() -> service.recordAttendance(COACH, 1, full));
        assertThat(cancelled.getCode()).isEqualTo(ErrorCode.SESSION_CANCELLED);
        assertThat(cancelled.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test
    void marksMustMatchTheRosterExactlyOnce() {
        BusinessException outside = failure(() -> service.recordAttendance(COACH, 1,
                request(5, AttendanceStatus.PRESENT, 9, AttendanceStatus.ABSENT)));
        assertThat(outside.getCode()).isEqualTo(ErrorCode.ATHLETE_NOT_IN_ROSTER);

        BusinessException incomplete = failure(() -> service.recordAttendance(COACH, 1,
                new RecordAttendanceRequest(List.of(new AttendanceMark(5, AttendanceStatus.PRESENT)))));
        assertThat(incomplete.getCode()).isEqualTo(ErrorCode.INCOMPLETE_ATTENDANCE);

        FieldValidationException repeated = catchThrowableOfType(FieldValidationException.class, () ->
                service.recordAttendance(COACH, 1, request(5, AttendanceStatus.PRESENT, 5, AttendanceStatus.ABSENT)));
        assertThat(repeated.getField()).isEqualTo("records");
        assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
    }

    @Test
    void aPastSessionCanBeRecordedAgainAfterCompletion() {
        session.reschedule(LocalDate.of(2026, 10, 1), session.getStartTime(), session.getEndTime());
        session.setStatus(SessionStatus.COMPLETED);

        assertThat(service.recordAttendance(COACH, 1,
                request(5, AttendanceStatus.ABSENT, 6, AttendanceStatus.ABSENT)).status())
                .isEqualTo(SessionStatus.COMPLETED);
    }

    private static RecordAttendanceRequest request(int first, AttendanceStatus firstStatus, int second,
                                                   AttendanceStatus secondStatus) {
        return new RecordAttendanceRequest(List.of(new AttendanceMark(first, firstStatus),
                new AttendanceMark(second, secondStatus)));
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
