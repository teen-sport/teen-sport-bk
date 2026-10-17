package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.SessionStatus;
import java.time.LocalDate;
import java.util.Collection;

/**
 * Filtros del calendario de sesiones (A3). {@code coachId} y {@code groupIds} limitan el alcance según el
 * rol y los fija el servicio, nunca el cliente.
 *
 * @param from         primera fecha, incluida
 * @param to           última fecha, incluida
 * @param groupId      filtro por grupo
 * @param disciplineId filtro por disciplina
 * @param status       filtro por estado
 * @param coachId      entrenador del grupo al que se limita la consulta, o {@code null}
 * @param groupIds     grupos a los que se limita la consulta, o {@code null} para no limitarla
 */
public record SessionFilter(LocalDate from, LocalDate to, Integer groupId, Integer disciplineId,
                            SessionStatus status, Integer coachId, Collection<Integer> groupIds) {
}
