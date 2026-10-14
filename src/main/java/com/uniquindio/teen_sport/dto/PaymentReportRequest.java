package com.uniquindio.teen_sport.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Reporte de un pago presencial por el acudiente (esquema {@code PaymentReportRequest}). El cuerpo es
 * opcional.
 *
 * @param paymentDate fecha en que el acudiente dice haber pagado; solo sirve para comprobar si ese mes
 *                    ya tiene un pago registrado. Sin ella se usa el mes actual
 * @param notes       nota para el administrador, opcional
 */
public record PaymentReportRequest(LocalDate paymentDate, @Size(max = 255) String notes) {
}
