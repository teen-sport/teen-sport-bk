package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class DateRangePolicyTest {

    private static final LocalDate OCTOBER_FIRST = LocalDate.of(2026, 10, 1);

    @Test
    void sessionRangesIncludeBothEndsUpToSixtyTwoDays() {
        assertThatCode(() -> DateRangePolicy.requireSessionRange(OCTOBER_FIRST, LocalDate.of(2026, 12, 1)))
                .doesNotThrowAnyException();
        assertThatCode(() -> DateRangePolicy.requireSessionRange(OCTOBER_FIRST, OCTOBER_FIRST))
                .doesNotThrowAnyException();

        BusinessException tooLong = catchThrowableOfType(BusinessException.class,
                () -> DateRangePolicy.requireSessionRange(OCTOBER_FIRST, LocalDate.of(2026, 12, 2)));

        assertThat(tooLong.getCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        assertThat(tooLong.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void reversedRangesAreRejectedAndOpenEndsAreAccepted() {
        BusinessException reversed = catchThrowableOfType(BusinessException.class,
                () -> DateRangePolicy.requireOrdered(OCTOBER_FIRST, OCTOBER_FIRST.minusDays(1)));

        assertThat(reversed.getCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        assertThatCode(() -> DateRangePolicy.requireOrdered(null, OCTOBER_FIRST)).doesNotThrowAnyException();
        assertThatCode(() -> DateRangePolicy.requireOrdered(OCTOBER_FIRST, null)).doesNotThrowAnyException();
    }

    @Test
    void reportRangesAllowAFullYear() {
        LocalDate lastDay = OCTOBER_FIRST.plusDays(DateRangePolicy.MAX_REPORT_RANGE_DAYS - 1L);
        assertThatCode(() -> DateRangePolicy.requireReportRange(OCTOBER_FIRST, lastDay)).doesNotThrowAnyException();

        BusinessException tooLong = catchThrowableOfType(BusinessException.class,
                () -> DateRangePolicy.requireReportRange(OCTOBER_FIRST, lastDay.plusDays(1)));
        BusinessException reversed = catchThrowableOfType(BusinessException.class,
                () -> DateRangePolicy.requireReportRange(OCTOBER_FIRST, OCTOBER_FIRST.minusDays(1)));

        assertThat(tooLong.getCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        assertThat(tooLong.getMessage()).isEqualTo(DateRangePolicy.REPORT_TOO_LONG_MESSAGE);
        assertThat(reversed.getMessage()).isEqualTo(DateRangePolicy.REVERSED_MESSAGE);
    }

    @Test
    void openReportRangesAllowMissingEndsAndValidateFullRanges() {
        LocalDate lastDay = OCTOBER_FIRST.plusDays(DateRangePolicy.MAX_REPORT_RANGE_DAYS - 1L);
        assertThatCode(() -> DateRangePolicy.requireOpenReportRange(OCTOBER_FIRST, lastDay))
                .doesNotThrowAnyException();
        assertThatCode(() -> DateRangePolicy.requireOpenReportRange(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> DateRangePolicy.requireOpenReportRange(OCTOBER_FIRST, null)).doesNotThrowAnyException();
        assertThatCode(() -> DateRangePolicy.requireOpenReportRange(null, OCTOBER_FIRST)).doesNotThrowAnyException();

        BusinessException tooLong = catchThrowableOfType(BusinessException.class,
                () -> DateRangePolicy.requireOpenReportRange(OCTOBER_FIRST, lastDay.plusDays(1)));
        BusinessException reversed = catchThrowableOfType(BusinessException.class,
                () -> DateRangePolicy.requireOpenReportRange(OCTOBER_FIRST, OCTOBER_FIRST.minusDays(1)));

        assertThat(tooLong.getMessage()).isEqualTo(DateRangePolicy.REPORT_TOO_LONG_MESSAGE);
        assertThat(reversed.getMessage()).isEqualTo(DateRangePolicy.REVERSED_MESSAGE);
    }
}
