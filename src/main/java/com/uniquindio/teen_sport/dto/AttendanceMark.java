package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Marca de un deportista (elemento de {@code RecordAttendanceRequest.records}).
 *
 * @param athleteId id del deportista
 * @param status    marca
 */
public record AttendanceMark(@NotNull Integer athleteId, @NotNull AttendanceStatus status) {
}
