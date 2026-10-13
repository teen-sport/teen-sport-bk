package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupOccupancy;
import com.uniquindio.teen_sport.service.SpotCounter.Occupancy;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpotCounterTest {

    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final SpotCounter counter = new SpotCounter(enrollmentRepository);

    @Test
    void lastSpotCanBeReservedButAFullGroupCannot() {
        Group group = group(7, 3);
        when(enrollmentRepository.countByGroupIdAndStatusIn(7, EnrollmentStatus.IN_FORCE)).thenReturn(2L);
        assertThatCode(() -> counter.requireFreeSpot(group)).doesNotThrowAnyException();

        when(enrollmentRepository.countByGroupIdAndStatusIn(7, EnrollmentStatus.IN_FORCE)).thenReturn(3L);
        BusinessException ex = catchThrowableOfType(BusinessException.class, () -> counter.requireFreeSpot(group));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.NO_AVAILABLE_SPOTS);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void occupancyIsComputedInOneQueryWithThePeriodAsText() {
        when(enrollmentRepository.countOccupancy(List.of(7, 8), "2026-11", EnrollmentStatus.IN_FORCE))
                .thenReturn(List.of(new Row(7, 1L, 3L)));

        Map<Integer, Occupancy> occupancy = counter.occupancy(List.of(7, 8), YearMonth.of(2026, 11));

        assertThat(occupancy).containsOnlyKeys(7);
        assertThat(occupancy.get(7)).isEqualTo(new Occupancy(1, 3));
    }

    @Test
    void availableSpotsDiscountEveryEnrollmentInForceAndNeverGoNegative() {
        Occupancy occupancy = new Occupancy(1, 3);

        assertThat(occupancy.available(5)).isEqualTo(2);
        assertThat(occupancy.available(2)).isZero();
        assertThat(Occupancy.EMPTY.available(15)).isEqualTo(15);
    }

    private static Group group(int id, int capacity) {
        Group group = new Group(new Discipline("Fútbol", null, BigDecimal.TEN), null, "A", capacity);
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private record Row(Integer getGroupId, Long getStartedBy, Long getInForce) implements GroupOccupancy {
    }
}
