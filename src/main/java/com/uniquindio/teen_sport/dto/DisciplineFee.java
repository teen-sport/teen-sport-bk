package com.uniquindio.teen_sport.dto;

import java.math.BigDecimal;

/**
 * Disciplina con su mensualidad, tal como la ve quien busca cupo (campo {@code discipline} del esquema
 * {@code GroupAvailability}).
 *
 * @param id         id de la disciplina
 * @param name       nombre
 * @param monthlyFee mensualidad en COP
 */
public record DisciplineFee(Integer id, String name, BigDecimal monthlyFee) {
}
