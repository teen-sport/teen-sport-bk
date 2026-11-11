package com.uniquindio.teen_sport.dto;

import java.time.LocalDate;

/**
 * Deportista con ausencias consecutivas en un grupo (R4).
 *
 * @param athlete             deportista
 * @param group               grupo
 * @param consecutiveAbsences marcas {@code ABSENT} seguidas desde la más reciente
 * @param lastAttendance      fecha de la última marca {@code PRESENT} considerada; {@code null} si no hay
 */
public record RecurrentAbsence(AthleteRef athlete, IdName group, int consecutiveAbsences, LocalDate lastAttendance) {
}
