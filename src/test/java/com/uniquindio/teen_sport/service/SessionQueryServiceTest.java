package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.SessionAthlete;
import com.uniquindio.teen_sport.dto.SessionResponse;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.SessionFilter;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import static com.uniquindio.teen_sport.service.SessionFixtures.ADMIN;
import static com.uniquindio.teen_sport.service.SessionFixtures.COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.GUARDIAN;
import static com.uniquindio.teen_sport.service.SessionFixtures.OTHER_COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionQueryServiceTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    private final TrainingSessionRepository sessionRepository = mock(TrainingSessionRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final SessionQueryService service = new SessionQueryService(sessionRepository, enrollmentRepository,
            new GroupAccessPolicy(), new AthleteAccessPolicy(), SessionFixtures.calendar());
    private Group group;

    @BeforeEach
    void setUp() {
        group = SessionFixtures.group();
    }

    @Test
    @SuppressWarnings("unchecked")
    void withoutDatesTheRangeIsTheCurrentWeek() {
        when(sessionRepository.findAll(any(Specification.class), eq(SessionQueryService.ORDER)))
                .thenReturn(List.of(SessionFixtures.session(1, group, TODAY)));

        List<SessionResponse> sessions = service.list(ADMIN, filter(null, null), null);

        assertThat(sessions).singleElement().satisfies(session -> assertThat(session.athletes()).isNull());
        verify(enrollmentRepository, never()).findByStatusAndAthleteId(any(), any());
    }

    @Test
    void rangesWithOneEndLastSevenDaysAndCannotExceedSixtyTwo() {
        assertThat(failure(() -> service.list(ADMIN, filter(TODAY, TODAY.minusDays(1)), null)).getCode())
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        assertThat(failure(() -> service.list(ADMIN, filter(TODAY, TODAY.plusDays(62)), null)).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(service.list(ADMIN, filter(TODAY, null), null)).isEmpty();
        assertThat(service.list(COACH, filter(null, TODAY), null)).isEmpty();
        assertThat(service.list(ADMIN, filter(TODAY, TODAY.plusDays(61)), null)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardiansOnlySeeSessionsOfEnrollmentsAlreadyStartedWithTheirAthletes() {
        Athlete sofia = SessionFixtures.athlete(5, "Sofía", "Prueba");
        Athlete ana = SessionFixtures.athlete(6, "Ana", "Prueba");
        Athlete later = SessionFixtures.athlete(8, "Luz", "Prueba");
        when(enrollmentRepository.findByStatusAndAthleteGuardianId(EnrollmentStatus.ACTIVE, 20)).thenReturn(List.of(
                SessionFixtures.activeEnrollment(sofia, group, OCTOBER),
                SessionFixtures.activeEnrollment(ana, group, OCTOBER.minusMonths(1)),
                SessionFixtures.activeEnrollment(later, group, OCTOBER.plusMonths(1))));
        TrainingSession october = SessionFixtures.session(1, group, TODAY);
        TrainingSession september = SessionFixtures.session(2, group, LocalDate.of(2026, 9, 30));
        when(sessionRepository.findAll(any(Specification.class), eq(SessionQueryService.ORDER)))
                .thenReturn(List.of(september, october));

        List<SessionResponse> sessions = service.list(GUARDIAN, filter(LocalDate.of(2026, 9, 28), TODAY), null);

        assertThat(sessions).extracting(SessionResponse::id).containsExactly(2, 1);
        assertThat(sessions.get(0).athletes()).extracting(SessionAthlete::id).containsExactly(6);
        assertThat(sessions.get(1).athletes()).extracting(SessionAthlete::firstNames).containsExactly("Ana", "Sofía");
        assertThat(service.list(GUARDIAN, filter(null, null), 99)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void athleteFilterWithoutActiveEnrollmentsReturnsNothing() {
        when(enrollmentRepository.findByStatusAndAthleteId(EnrollmentStatus.ACTIVE, 5)).thenReturn(List.of());

        assertThat(service.list(ADMIN, filter(null, null), 5)).isEmpty();
        verify(sessionRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void detailIsLimitedToTheScopeOfEachRole() {
        TrainingSession session = SessionFixtures.session(1, group, TODAY);
        when(sessionRepository.findDetailedById(1)).thenReturn(Optional.of(session));

        assertThat(service.get(ADMIN, 1).athletes()).isNull();
        assertThat(service.get(COACH, 1).id()).isEqualTo(1);
        assertThat(failure(() -> service.get(OTHER_COACH, 1)).getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(failure(() -> service.get(GUARDIAN, 1)).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        when(enrollmentRepository.findByStatusAndAthleteGuardianId(EnrollmentStatus.ACTIVE, 20)).thenReturn(List.of(
                SessionFixtures.activeEnrollment(SessionFixtures.athlete(5, "Sofía", "Prueba"), group, OCTOBER)));
        assertThat(service.get(GUARDIAN, 1).athletes()).extracting(SessionAthlete::id).containsExactly(5);
        assertThat(failure(() -> service.get(ADMIN, 2)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static SessionFilter filter(LocalDate from, LocalDate to) {
        return new SessionFilter(from, to, null, null, null, null, null);
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
