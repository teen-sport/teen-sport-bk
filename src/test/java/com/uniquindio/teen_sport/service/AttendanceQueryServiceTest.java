package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.AttendanceRecordResponse;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.AttendanceRecord;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.AttendanceFilter;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static com.uniquindio.teen_sport.service.SessionFixtures.ADMIN;
import static com.uniquindio.teen_sport.service.SessionFixtures.COACH;
import static com.uniquindio.teen_sport.service.SessionFixtures.GUARDIAN;
import static com.uniquindio.teen_sport.service.SessionFixtures.NOW;
import static com.uniquindio.teen_sport.service.SessionFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AttendanceQueryServiceTest {

    private final AttendanceRecordRepository attendanceRepository = mock(AttendanceRecordRepository.class);
    private final AttendanceQueryService service = new AttendanceQueryService(attendanceRepository,
            new GroupAccessPolicy(), new AthleteAccessPolicy());

    @Test
    @SuppressWarnings("unchecked")
    void listsTheMostRecentSessionsFirstWithTheContractFields() {
        TrainingSession session = SessionFixtures.session(1, SessionFixtures.group(), TODAY);
        AttendanceRecord attendance = new AttendanceRecord(session,
                SessionFixtures.athlete(5, "Sofía", "Prueba"), AttendanceStatus.PRESENT, NOW);
        when(attendanceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(attendance), PageRequest.of(0, 20), 1));

        PageResponse<AttendanceRecordResponse> page = service.list(ADMIN, filter(null, null), 0, 20);

        assertThat(page.totalElements()).isEqualTo(1);
        AttendanceRecordResponse item = page.content().get(0);
        assertThat(item.sessionId()).isEqualTo(1);
        assertThat(item.date()).isEqualTo(TODAY);
        assertThat(item.group().id()).isEqualTo(7);
        assertThat(item.discipline().id()).isEqualTo(3);
        assertThat(item.athlete().lastNames()).isEqualTo("Prueba");
        assertThat(item.recordedAt()).isEqualTo(NOW);
        verify(attendanceRepository).findAll(any(Specification.class),
                eq(PageRequest.of(0, 20, AttendanceQueryService.ORDER)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void coachesAndGuardiansAreLimitedToTheirScope() {
        when(attendanceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.list(COACH, filter(null, null), 0, 20).content()).isEmpty();
        assertThat(service.list(GUARDIAN, filter(TODAY, TODAY), 0, 20).content()).isEmpty();
    }

    @Test
    void reversedRangeIsRejected() {
        BusinessException failure = catchThrowableOfType(BusinessException.class,
                () -> service.list(ADMIN, filter(TODAY, TODAY.minusDays(1)), 0, 20));

        assertThat(failure.getCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        verifyNoInteractions(attendanceRepository);
    }

    private static AttendanceFilter filter(LocalDate from, LocalDate to) {
        return new AttendanceFilter(null, null, from, to, null, 99, 99);
    }
}
