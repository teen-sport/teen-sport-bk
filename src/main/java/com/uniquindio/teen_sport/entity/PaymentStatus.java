package com.uniquindio.teen_sport.entity;

/**
 * Estado de pago de una matrícula (esquema {@code PaymentStatus} del contrato). Solo se alcanza
 * {@code UP_TO_DATE} registrando un pago.
 */
public enum PaymentStatus {
    UP_TO_DATE,
    OVERDUE
}
