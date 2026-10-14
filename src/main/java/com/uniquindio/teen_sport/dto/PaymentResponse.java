package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Pago registrado (esquema {@code Payment}).
 *
 * @param id               id
 * @param enrollmentId     matrícula pagada
 * @param athlete          deportista de la matrícula
 * @param discipline       disciplina de la matrícula
 * @param period           mes que cubre el pago, {@code YYYY-MM} de {@code paymentDate}
 * @param amountPaid       monto en COP
 * @param paymentDate      fecha del pago
 * @param receiptReference referencia del recibo
 * @param notes            notas
 * @param registeredBy     administrador que registró el pago (id de su cuenta)
 * @param createdAt        fecha de registro
 */
public record PaymentResponse(Integer id, Integer enrollmentId, AthleteRef athlete, IdName discipline,
                              @JsonFormat(pattern = "yyyy-MM") YearMonth period, BigDecimal amountPaid,
                              LocalDate paymentDate, String receiptReference, String notes, PersonRef registeredBy,
                              Instant createdAt) {
}
