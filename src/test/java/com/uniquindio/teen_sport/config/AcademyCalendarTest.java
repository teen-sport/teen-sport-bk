package com.uniquindio.teen_sport.config;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcademyCalendarTest {

    @Test
    void todayUsesTheAcademyZoneNotUtc() {
        // 9 p. m. del 12 de octubre en Bogotá (UTC-5) ya es 13 de octubre en UTC.
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T02:00:00Z"), ZoneOffset.UTC);

        AcademyCalendar calendar = new AcademyCalendar(clock, "America/Bogota");

        assertThat(calendar.today()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(calendar.zone().getId()).isEqualTo("America/Bogota");
    }

    @Test
    void invalidZonePreventsStartup() {
        Clock clock = Clock.systemUTC();

        assertThatThrownBy(() -> new AcademyCalendar(clock, "Marte/Olympus")).isInstanceOf(DateTimeException.class);
    }
}
