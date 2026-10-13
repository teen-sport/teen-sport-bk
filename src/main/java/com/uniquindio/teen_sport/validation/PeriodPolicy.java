package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;

/**
 * Regla de los periodos de matrícula ({@code YYYY-MM}): no pueden ser anteriores al mes actual de la
 * academia. El mes actual sí se admite.
 */
public final class PeriodPolicy {

    static final String PERIOD_IN_PAST_MESSAGE = "El periodo no puede ser anterior al mes actual.";

    private PeriodPolicy() {
    }

    /**
     * @param period  periodo solicitado
     * @param current mes actual en la zona de la academia
     * @throws BusinessException 422 {@code PERIOD_IN_PAST} si el periodo ya pasó
     */
    public static void requireNotPast(YearMonth period, YearMonth current) {
        if (period.isBefore(current)) {
            throw new BusinessException(ErrorCode.PERIOD_IN_PAST, HttpStatus.UNPROCESSABLE_CONTENT,
                    PERIOD_IN_PAST_MESSAGE);
        }
    }
}
