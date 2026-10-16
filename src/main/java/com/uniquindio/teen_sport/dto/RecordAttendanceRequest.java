package com.uniquindio.teen_sport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Asistencia de toda la lista de una sesión (esquema {@code RecordAttendanceRequest}).
 *
 * @param records una marca por deportista de la lista
 */
public record RecordAttendanceRequest(@NotEmpty List<@Valid @NotNull AttendanceMark> records) {
}
