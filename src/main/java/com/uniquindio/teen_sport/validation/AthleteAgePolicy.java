package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.http.HttpStatus;

/**
 * Rango de edad de los deportistas de la academia: de {@value #MIN_AGE} a {@value #MAX_AGE} años
 * cumplidos en la fecha de la solicitud.
 *
 * <p>La edad es la de años cumplidos: quien cumple 12 años hoy ya es elegible y quien cumple 18 hoy
 * ya no lo es. Una fecha de nacimiento futura da una edad negativa y queda fuera del rango.</p>
 */
public final class AthleteAgePolicy {

    /** Edad mínima, inclusive. */
    public static final int MIN_AGE = 12;

    /** Edad máxima, inclusive. */
    public static final int MAX_AGE = 17;

    static final String AGE_OUT_OF_RANGE_MESSAGE = "El deportista debe tener entre 12 y 17 años.";

    private AthleteAgePolicy() {
    }

    /**
     * @param birthDate fecha de nacimiento
     * @param today     fecha de referencia
     * @return años cumplidos en la fecha de referencia
     */
    public static int ageOn(LocalDate birthDate, LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    /**
     * @param birthDate fecha de nacimiento
     * @param today     fecha de la solicitud, en la zona de la academia
     * @throws BusinessException 422 {@code AGE_OUT_OF_RANGE} si la edad está fuera del rango
     */
    public static void requireEligible(LocalDate birthDate, LocalDate today) {
        int age = ageOn(birthDate, today);
        if (birthDate.isAfter(today) || age < MIN_AGE || age > MAX_AGE) {
            throw new BusinessException(ErrorCode.AGE_OUT_OF_RANGE, HttpStatus.UNPROCESSABLE_CONTENT,
                    AGE_OUT_OF_RANGE_MESSAGE);
        }
    }
}
