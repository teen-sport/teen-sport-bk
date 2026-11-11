package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.NotificationType;
import com.uniquindio.teen_sport.integration.EmailMessage;

/**
 * Evento de aplicación con un correo por enviar. Lo publica {@link NotificationService} dentro de la
 * transacción del caso de uso y lo atiende {@link NotificationDispatcher} cuando esta termina.
 *
 * @param userId        id de la cuenta destinataria
 * @param type          evento de negocio
 * @param message       correo ya compuesto
 * @param afterRollback {@code true} si el correo informa de una operación rechazada y debe enviarse
 *                      cuando la transacción se revierte ({@code NO_SPOTS_AVAILABLE}); {@code false}
 *                      si solo debe enviarse si la transacción se confirma
 */
public record NotificationRequested(Integer userId, NotificationType type, EmailMessage message,
                                    boolean afterRollback) {
}
