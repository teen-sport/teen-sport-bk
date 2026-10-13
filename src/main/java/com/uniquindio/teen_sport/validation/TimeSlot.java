package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.entity.Schedule;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Franja semanal: día y rango de horas {@code [inicio, fin)}.
 *
 * <p>Dos franjas se cruzan si son del mismo día y {@code inicio < finDeLaOtra} y
 * {@code inicioDeLaOtra < fin}. Por eso las franjas contiguas (16:00–17:00 y 17:00–18:00) no se
 * cruzan.</p>
 *
 * @param day   día de la semana
 * @param start hora de inicio
 * @param end   hora de fin, posterior al inicio
 */
public record TimeSlot(DayOfWeek day, LocalTime start, LocalTime end) {

    /**
     * @param schedule franja guardada
     * @return la franja equivalente
     */
    public static TimeSlot of(Schedule schedule) {
        return new TimeSlot(schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime());
    }

    /**
     * @return {@code true} si la hora de fin es posterior a la de inicio
     */
    public boolean hasValidRange() {
        return end.isAfter(start);
    }

    /**
     * @param other otra franja
     * @return {@code true} si ambas franjas comparten algún momento
     */
    public boolean overlaps(TimeSlot other) {
        return day.equals(other.day) && start.isBefore(other.end) && other.start.isBefore(end);
    }
}
