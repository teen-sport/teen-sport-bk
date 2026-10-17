package com.uniquindio.teen_sport.entity;

/**
 * Estado de una sesión de entrenamiento (esquema {@code SessionStatus} del contrato). Una sesión nace
 * {@code SCHEDULED}; pasa a {@code COMPLETED} al registrar su asistencia (A8) o a {@code CANCELLED} al
 * cancelarla (A6).
 */
public enum SessionStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
