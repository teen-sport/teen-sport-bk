package com.uniquindio.teen_sport.entity;

/**
 * Estado de entrega de una notificación ({@code notifications.delivery_status}).
 */
public enum DeliveryStatus {
    /** Registrada; aún no se confirmó el envío. */
    PENDING,
    /** El proveedor de correo aceptó el mensaje. */
    SENT,
    /** Se agotaron los intentos o el proveedor rechazó el mensaje de forma definitiva. */
    FAILED
}
