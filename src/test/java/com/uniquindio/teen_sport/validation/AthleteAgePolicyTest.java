package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class AthleteAgePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @ParameterizedTest(name = "nacido {0} → elegible")
    @CsvSource({
            "2014-10-12",   // cumple 12 hoy
            "2008-10-13",   // cumple 18 mañana: hoy tiene 17
            "2012-01-01"
    })
    void acceptsAgesFromTwelveToSeventeen(LocalDate birthDate) {
        assertThatCode(() -> AthleteAgePolicy.requireEligible(birthDate, TODAY)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "nacido {0} → fuera de rango")
    @CsvSource({
            "2014-10-13",   // cumple 12 mañana: hoy tiene 11
            "2008-10-12",   // cumple 18 hoy
            "2000-01-01",
            "2027-01-01"    // fecha futura
    })
    void rejectsAgesOutsideTheRange(LocalDate birthDate) {
        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> AthleteAgePolicy.requireEligible(birthDate, TODAY));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.AGE_OUT_OF_RANGE);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void leapDayBirthdayCountsTheYearOnTheLastDayOfFebruary() {
        LocalDate leapDay = LocalDate.of(2012, 2, 29);

        assertThat(AthleteAgePolicy.ageOn(leapDay, LocalDate.of(2024, 2, 28))).isEqualTo(11);
        assertThat(AthleteAgePolicy.ageOn(leapDay, LocalDate.of(2024, 2, 29))).isEqualTo(12);
        assertThat(AthleteAgePolicy.ageOn(leapDay, LocalDate.of(2025, 2, 28))).isEqualTo(12);
    }
}
