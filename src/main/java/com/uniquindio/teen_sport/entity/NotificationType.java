package com.uniquindio.teen_sport.entity;

/**
 * Eventos que envían un correo (catálogo aprobado el 2026-11-10). Se guardan como texto en
 * {@code notifications.type}.
 */
public enum NotificationType {
    /** Registro de un acudiente (U2). */
    ACCOUNT_REGISTERED,
    /** Matrícula confirmada (E12). */
    ENROLLMENT_CONFIRMED,
    /** Matrícula rechazada (E13). */
    ENROLLMENT_REJECTED,
    /** Solicitud de matrícula sin cupo (E6). */
    NO_SPOTS_AVAILABLE,
    /** Sesiones generadas con aviso a los acudientes (A2 con {@code notify}). */
    SCHEDULE_PUBLISHED,
    /** Sesión reprogramada o cancelada (A5, A6). */
    SCHEDULE_CHANGE
}
