package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Reglas de fecha y hora de una sesión, compartidas por A1, A2 y A5.
 *
 * <ul>
 *   <li>La hora de fin debe ser posterior a la de inicio ({@code INVALID_TIME_RANGE}).</li>
 *   <li>La fecha no puede ser anterior a hoy en la zona de la academia ({@code DATE_IN_PAST}); hoy se
 *       admite aunque la hora ya haya pasado.</li>
 *   <li>Un grupo no puede tener dos sesiones con la misma fecha y hora de inicio
 *       ({@code DUPLICATE_SESSION}). Quien llama debe tener bloqueado el grupo para que la comprobación
 *       no compita con otra creación simultánea.</li>
 * </ul>
 */
@Component
public class SessionSlotPolicy {

    static final String INVALID_TIME_RANGE_MESSAGE = "La hora de fin debe ser posterior a la de inicio.";
    static final String DATE_IN_PAST_MESSAGE = "La fecha de la sesión no puede ser anterior a hoy.";
    static final String DUPLICATE_MESSAGE = "El grupo ya tiene una sesión en esa fecha y hora de inicio.";

    private final TrainingSessionRepository sessionRepository;
    private final AcademyCalendar calendar;

    /**
     * @param sessionRepository sesiones
     * @param calendar          fecha de la academia
     */
    public SessionSlotPolicy(TrainingSessionRepository sessionRepository, AcademyCalendar calendar) {
        this.sessionRepository = sessionRepository;
        this.calendar = calendar;
    }

    /**
     * @param date      fecha
     * @param startTime hora de inicio
     * @param endTime   hora de fin
     * @throws BusinessException 422 {@code INVALID_TIME_RANGE} o {@code DATE_IN_PAST}
     */
    public void requireValidSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE, HttpStatus.UNPROCESSABLE_CONTENT,
                    INVALID_TIME_RANGE_MESSAGE);
        }
        requireNotPast(date);
    }

    /**
     * @param date fecha
     * @throws BusinessException 422 {@code DATE_IN_PAST} si es anterior a hoy
     */
    public void requireNotPast(LocalDate date) {
        if (date.isBefore(calendar.today())) {
            throw new BusinessException(ErrorCode.DATE_IN_PAST, HttpStatus.UNPROCESSABLE_CONTENT, DATE_IN_PAST_MESSAGE);
        }
    }

    /**
     * @param groupId          id del grupo, ya bloqueado
     * @param date             fecha
     * @param startTime        hora de inicio
     * @param excludedSession  sesión que se reprograma, o {@code null} al crear
     * @throws BusinessException 409 {@code DUPLICATE_SESSION}
     */
    public void requireFree(Integer groupId, LocalDate date, LocalTime startTime, Integer excludedSession) {
        boolean taken = excludedSession == null
                ? sessionRepository.existsByGroupIdAndSessionDateAndStartTime(groupId, date, startTime)
                : sessionRepository.existsByGroupIdAndSessionDateAndStartTimeAndIdNot(groupId, date, startTime,
                excludedSession);
        if (taken) {
            throw new BusinessException(ErrorCode.DUPLICATE_SESSION, HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }
    }
}
