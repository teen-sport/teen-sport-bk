package com.uniquindio.teen_sport.dto;

import java.util.List;

/**
 * Disponibilidad de un grupo en un periodo (esquema {@code GroupAvailability}).
 *
 * @param groupId        id del grupo
 * @param groupName      nombre del grupo
 * @param discipline     disciplina con su mensualidad
 * @param coach          entrenador a cargo (id de entrenador)
 * @param schedules      franjas semanales, ordenadas por día y hora
 * @param maxCapacity    capacidad máxima
 * @param occupiedSpots  matrículas vigentes que empezaron en el periodo o antes
 * @param availableSpots cupos que aún se pueden reservar desde el periodo: la capacidad menos todas las
 *                       matrículas vigentes, porque cada una ocupa cupo en todos los meses desde su inicio
 */
public record GroupAvailability(Integer groupId, String groupName, DisciplineFee discipline, PersonRef coach,
                                List<ScheduleResponse> schedules, int maxCapacity, int occupiedSpots,
                                int availableSpots) {
}
