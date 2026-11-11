package com.uniquindio.teen_sport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Porcentajes e indicadores de los reportes (R1–R3), redondeados a un decimal con redondeo hacia el
 * valor más cercano ({@code 66.66…} → {@code 66.7}).
 */
final class Percentages {

    private static final int DECIMALS = 1;

    private Percentages() {
    }

    /**
     * Para indicadores que el contrato exige siempre: sin base de cálculo valen 0.
     *
     * @param part  casos que cumplen
     * @param total casos posibles
     * @return el porcentaje, o 0 si {@code total} es 0
     */
    static double of(long part, long total) {
        return total == 0 ? 0.0 : round(100.0 * part / total);
    }

    /**
     * Para indicadores que el contrato admite como nulos: sin base de cálculo no hay valor.
     *
     * @param part  casos que cumplen
     * @param total casos posibles
     * @return el porcentaje, o {@code null} si {@code total} es 0
     */
    static Double ofOrNull(long part, long total) {
        return total == 0 ? null : round(100.0 * part / total);
    }

    /**
     * @param value valor a redondear
     * @return el valor con un decimal
     */
    static double round(double value) {
        return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).doubleValue();
    }
}
