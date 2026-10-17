package com.uniquindio.teen_sport.validation;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSlotTest {

    private static final TimeSlot MONDAY_16_TO_18 = slot(DayOfWeek.MONDAY, "16:00", "18:00");

    @ParameterizedTest(name = "{0}-{1} se cruza con 16:00-18:00: {2}")
    @CsvSource({
            "15:00, 16:00, false",  // termina justo cuando empieza: contigua
            "18:00, 19:00, false",  // empieza justo cuando termina: contigua
            "14:00, 15:30, false",
            "15:00, 16:30, true",   // se solapa al inicio
            "17:30, 19:00, true",   // se solapa al final
            "16:30, 17:30, true",   // contenida
            "15:00, 19:00, true",   // la contiene
            "16:00, 18:00, true"    // idéntica
    })
    void detectsOverlapOnTheSameDay(String start, String end, boolean expected) {
        TimeSlot other = slot(DayOfWeek.MONDAY, start, end);

        assertThat(MONDAY_16_TO_18.overlaps(other)).isEqualTo(expected);
        assertThat(other.overlaps(MONDAY_16_TO_18)).isEqualTo(expected);
    }

    @Test
    void slotsOnDifferentDaysNeverOverlap() {
        assertThat(MONDAY_16_TO_18.overlaps(slot(DayOfWeek.TUESDAY, "16:00", "18:00"))).isFalse();
    }

    @Test
    void endMustBeAfterStart() {
        assertThat(MONDAY_16_TO_18.hasValidRange()).isTrue();
        assertThat(slot(DayOfWeek.MONDAY, "18:00", "18:00").hasValidRange()).isFalse();
        assertThat(slot(DayOfWeek.MONDAY, "18:00", "16:00").hasValidRange()).isFalse();
    }

    private static TimeSlot slot(DayOfWeek day, String start, String end) {
        return new TimeSlot(day, LocalTime.parse(start), LocalTime.parse(end));
    }
}
