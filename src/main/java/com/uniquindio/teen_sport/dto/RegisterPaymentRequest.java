package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registro de un pago presencial (esquema {@code RegisterPaymentRequest}), en E17 o dentro de E12.
 *
 * <p>El monto debe ser mayor que 0 y caber en {@code DECIMAL(10,2)}. La referencia del recibo se
 * limita a 50 caracteres como indica el contrato, aunque la columna del MER admite 100.</p>
 *
 * @param amountPaid       monto en COP
 * @param paymentDate      fecha del pago; no puede ser futura
 * @param receiptReference referencia del recibo, opcional
 * @param notes            notas, opcionales
 */
public record RegisterPaymentRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal amountPaid,
        @NotNull LocalDate paymentDate,
        @Size(max = 50) String receiptReference,
        @Size(max = 255) String notes) {
}
