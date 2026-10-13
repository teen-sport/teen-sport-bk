package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Creación o edición de una disciplina (esquema {@code DisciplineRequest}).
 *
 * <p>La mensualidad debe ser mayor que 0 y caber en {@code DECIMAL(10,2)}: hasta 8 dígitos enteros
 * y 2 decimales (máximo 99.999.999,99 COP).</p>
 *
 * @param name        nombre, único sin distinguir mayúsculas
 * @param description descripción, opcional
 * @param monthlyFee  mensualidad en COP
 */
public record DisciplineRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal monthlyFee) {
}
