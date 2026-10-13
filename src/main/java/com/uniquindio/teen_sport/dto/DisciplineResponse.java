package com.uniquindio.teen_sport.dto;

import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.EntityStatus;
import java.math.BigDecimal;

/**
 * Disciplina en las respuestas de la API (esquema {@code Discipline}).
 *
 * @param id          id
 * @param name        nombre
 * @param description descripción
 * @param monthlyFee  mensualidad en COP
 * @param status      estado
 */
public record DisciplineResponse(Integer id, String name, String description, BigDecimal monthlyFee,
                                 EntityStatus status) {

    /**
     * @param discipline entidad
     * @return la disciplina en el formato del contrato
     */
    public static DisciplineResponse from(Discipline discipline) {
        return new DisciplineResponse(discipline.getId(), discipline.getName(), discipline.getDescription(),
                discipline.getMonthlyFee(), discipline.getStatus());
    }
}
