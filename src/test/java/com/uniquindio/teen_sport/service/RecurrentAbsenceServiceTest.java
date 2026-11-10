package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.AthleteRef;
import com.uniquindio.teen_sport.dto.IdName;
import com.uniquindio.teen_sport.dto.RecurrentAbsence;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.AthleteGroupMark;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.uniquindio.teen_sport.service.SessionFixtures.ADMIN;
import static com.uniquindio.teen_sport.service.SessionFixtures.COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.GUARDIAN;
import static com.uniquindio.teen_sport.service.SessionFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R4: ausencias consecutivas.
 */
class RecurrentAbsenceServiceTest {

    private static final AttendanceStatus ABSENT = AttendanceStatus.ABSENT;
    private static final AttendanceStatus PRESENT = AttendanceStatus.PRESENT;

    private final AttendanceRecordRepository attendanceRepository = mock(AttendanceRecordRepository.class);
    private RecurrentAbsenceService service;

    @BeforeEach
    void setUp() {
        service = new RecurrentAbsenceService(attendanceRepository, new GroupAccessPolicy());
    }

    @Test
    void countsTheLatestRunOfAbsencesPerAthleteAndGroupAndSortsByLongestRun() {
        List<AthleteGroupMark> marks = List.of(
                        // Ana en el grupo 7: tres ausencias y luego asistió hace 5 días.
                        mark(1, "Ana", "Pérez", 7, ABSENT, 0), mark(1, "Ana", "Pérez", 7, ABSENT, 1),
                        mark(1, "Ana", "Pérez", 7, ABSENT, 2), mark(1, "Ana", "Pérez", 7, PRESENT, 5),
                        mark(1, "Ana", "Pérez", 7, ABSENT, 6),
                        // Ana en el grupo 9: asistió a la última sesión.
                        mark(1, "Ana", "Pérez", 9, PRESENT, 0), mark(1, "Ana", "Pérez", 9, ABSENT, 1),
                        // Luis en el grupo 7: cuatro ausencias, nunca asistió.
                        mark(2, "Luis", "Álvarez", 7, ABSENT, 0), mark(2, "Luis", "Álvarez", 7, ABSENT, 1),
                        mark(2, "Luis", "Álvarez", 7, ABSENT, 2), mark(2, "Luis", "Álvarez", 7, ABSENT, 3),
                        // Eva en el grupo 7: solo dos ausencias, por debajo del mínimo.
                        mark(3, "Eva", "Ruiz", 7, ABSENT, 0), mark(3, "Eva", "Ruiz", 7, ABSENT, 1));
        when(attendanceRepository.findMarksOfEnrolledAthletes(null, null, null, EnrollmentStatus.IN_FORCE))
                .thenReturn(marks);

        List<RecurrentAbsence> absences = service.recurrentAbsences(ADMIN, 3, null, null);

        assertThat(absences).containsExactly(
                new RecurrentAbsence(new AthleteRef(2, "Luis", "Álvarez"), new IdName(7, "Grupo 7"), 4, null),
                new RecurrentAbsence(new AthleteRef(1, "Ana", "Pérez"), new IdName(7, "Grupo 7"), 3,
                        TODAY.minusDays(5)));
    }

    @Test
    void equalRunsAreOrderedByLastNames() {
        List<AthleteGroupMark> marks = List.of(mark(1, "Ana", "Pérez", 7, ABSENT, 0),
                mark(2, "Luis", "Álvarez", 7, ABSENT, 0));
        when(attendanceRepository.findMarksOfEnrolledAthletes(null, null, null, EnrollmentStatus.IN_FORCE))
                .thenReturn(marks);

        List<RecurrentAbsence> absences = service.recurrentAbsences(ADMIN, 1, null, null);

        assertThat(absences).extracting(absence -> absence.athlete().id()).containsExactly(2, 1);
    }

    @Test
    void equalRunsAndLastNamesAreOrderedByFirstNamesThenAthleteThenGroup() {
        List<AthleteGroupMark> marks = List.of(mark(5, "Sara", "Gómez", 9, ABSENT, 0),
                mark(5, "Sara", "Gómez", 7, ABSENT, 0), mark(4, "sara", "gomez", 7, ABSENT, 0),
                mark(6, "Álex", "Gómez", 7, ABSENT, 0));
        when(attendanceRepository.findMarksOfEnrolledAthletes(null, null, null, EnrollmentStatus.IN_FORCE))
                .thenReturn(marks);

        List<RecurrentAbsence> absences = service.recurrentAbsences(ADMIN, 1, null, null);

        assertThat(absences).extracting(absence -> List.of(absence.athlete().id(), absence.group().id()))
                .containsExactly(List.of(6, 7), List.of(4, 7), List.of(5, 7), List.of(5, 9));
    }

    @Test
    void coachIsLimitedToTheirGroupsAndFiltersArePassedThrough() {
        LocalDate from = TODAY.minusDays(30);

        assertThat(service.recurrentAbsences(COACH, 2, 7, from)).isEmpty();

        verify(attendanceRepository).findMarksOfEnrolledAthletes(7, COACH.coachId(), from, EnrollmentStatus.IN_FORCE);
    }

    @Test
    void guardiansCannotQueryRecurrentAbsences() {
        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.recurrentAbsences(GUARDIAN, 3, null, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    private static AthleteGroupMark mark(int athleteId, String firstNames, String lastNames, int groupId,
                                         AttendanceStatus status, int daysAgo) {
        AthleteGroupMark mark = mock(AthleteGroupMark.class);
        when(mark.getAthleteId()).thenReturn(athleteId);
        when(mark.getFirstNames()).thenReturn(firstNames);
        when(mark.getLastNames()).thenReturn(lastNames);
        when(mark.getGroupId()).thenReturn(groupId);
        when(mark.getGroupName()).thenReturn("Grupo " + groupId);
        when(mark.getStatus()).thenReturn(status);
        when(mark.getSessionDate()).thenReturn(TODAY.minusDays(daysAgo));
        return mark;
    }
}
