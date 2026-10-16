package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;

/**
 * Rangos de fechas de sesiones y asistencia (A2, A3, A9). Ambos extremos se incluyen: del 1 de octubre al
 * 1 de diciembre hay 62 días.
 */
public final class DateRangePolicy {

    /** Días que puede abarcar un rango de sesiones (A2, A3). */
    public static final int MAX_SESSION_RANGE_DAYS = 62;

    static final String REVERSED_MESSAGE = "La fecha inicial no puede ser posterior a la final.";
    static final String TOO_LONG_MESSAGE = "El rango de fechas no puede superar "
            + MAX_SESSION_RANGE_DAYS + " días.";

    private DateRangePolicy() {
    }

    /**
     * @param from primera fecha, o {@code null}
     * @param to   última fecha, o {@code null}
     * @throws BusinessException 422 {@code INVALID_DATE_RANGE} si ambas existen y {@code from} es posterior a {@code to}
     */
    public static void requireOrdered(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw invalid(REVERSED_MESSAGE);
        }
    }

    /**
     * @param from primera fecha
     * @param to   última fecha
     * @throws BusinessException 422 {@code INVALID_DATE_RANGE} si el rango está invertido o supera
     *                           {@value #MAX_SESSION_RANGE_DAYS} días
     */
    public static void requireSessionRange(LocalDate from, LocalDate to) {
        requireOrdered(from, to);
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_SESSION_RANGE_DAYS) {
            throw invalid(TOO_LONG_MESSAGE);
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_DATE_RANGE, HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
