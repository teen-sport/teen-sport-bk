package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.EntityStatus;
import java.util.List;

/**
 * Grupo con sus franjas semanales (esquema {@code Group}).
 *
 * @param id          id
 * @param name        nombre
 * @param discipline  disciplina
 * @param coach       entrenador a cargo (id de entrenador)
 * @param maxCapacity capacidad máxima
 * @param status      estado
 * @param schedules   franjas semanales, ordenadas por día y hora
 */
public record GroupResponse(Integer id, String name, IdName discipline, PersonRef coach, Integer maxCapacity,
                            EntityStatus status, List<ScheduleResponse> schedules) {
}
