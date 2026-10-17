package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PeriodPolicyTest {

    private static final YearMonth CURRENT = YearMonth.of(2026, 10);

    @Test
    void currentAndFutureMonthsAreAccepted() {
        assertThatCode(() -> PeriodPolicy.requireNotPast(CURRENT, CURRENT)).doesNotThrowAnyException();
        assertThatCode(() -> PeriodPolicy.requireNotPast(YearMonth.of(2027, 1), CURRENT)).doesNotThrowAnyException();
    }

    @Test
    void previousMonthIsInThePast() {
        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> PeriodPolicy.requireNotPast(YearMonth.of(2026, 9), CURRENT));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERIOD_IN_PAST);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
