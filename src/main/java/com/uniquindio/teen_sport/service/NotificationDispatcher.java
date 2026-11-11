package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.DeliveryStatus;
import com.uniquindio.teen_sport.integration.EmailDeliveryException;
import com.uniquindio.teen_sport.integration.EmailSender;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía las notificaciones cuando termina la transacción que las pidió, en un hilo aparte para no
 * retrasar la respuesta HTTP (ADR-09).
 *
 * <p>Ciclo de vida (decisión del 2026-11-10): se registra {@code PENDING} sin intentos, se intenta enviar
 * hasta {@code app.notifications.max-attempts} veces y queda {@code SENT} en cuanto el proveedor acepta
 * el correo, o {@code FAILED} si el fallo es definitivo o se agotan los intentos. {@code attempts}
 * guarda los intentos hechos. Nunca se marca {@code SENT} sin la aceptación del proveedor.</p>
 */
@Component
public class NotificationDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final EmailSender emailSender;
    private final NotificationLog notificationLog;
    private final NotificationProperties properties;

    /**
     * @param emailSender     proveedor de correo
     * @param notificationLog registro de las notificaciones
     * @param properties      política de reintentos
     */
    public NotificationDispatcher(EmailSender emailSender, NotificationLog notificationLog,
                                  NotificationProperties properties) {
        this.emailSender = emailSender;
        this.notificationLog = notificationLog;
        this.properties = properties;
    }

    /**
     * Correos de operaciones exitosas: solo si la transacción se confirma.
     *
     * @param request notificación por enviar
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, condition = "!#p0.afterRollback()")
    public void onCommit(NotificationRequested request) {
        deliver(request);
    }

    /**
     * Correos que informan de una operación rechazada: solo si la transacción se revierte.
     *
     * @param request notificación por enviar
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK, condition = "#p0.afterRollback()")
    public void onRollback(NotificationRequested request) {
        deliver(request);
    }

    /**
     * Registra la notificación e intenta enviarla según la política de reintentos.
     *
     * @param request notificación por enviar
     */
    void deliver(NotificationRequested request) {
        Integer notificationId = notificationLog.open(request);
        int attempt = 1;
        while (tryOnce(request, notificationId, attempt)) {
            if (!pauseAfter(attempt)) {
                notificationLog.recordAttempt(notificationId, DeliveryStatus.FAILED, attempt);
                LOG.warn("Notificación {} ({}) interrumpida tras {} intento(s)", notificationId, request.type(),
                        attempt);
                return;
            }
            attempt++;
        }
    }

    /**
     * Hace un intento y guarda su resultado.
     *
     * @return {@code true} si falló de forma transitoria y quedan intentos
     */
    private boolean tryOnce(NotificationRequested request, Integer notificationId, int attempt) {
        try {
            emailSender.send(request.message());
            notificationLog.recordAttempt(notificationId, DeliveryStatus.SENT, attempt);
            return false;
        } catch (EmailDeliveryException e) {
            boolean retry = e.isRetryable() && attempt < properties.maxAttempts();
            notificationLog.recordAttempt(notificationId, retry ? DeliveryStatus.PENDING : DeliveryStatus.FAILED,
                    attempt);
            if (!retry) {
                LOG.warn("Notificación {} ({}) fallida tras {} intento(s): {}", notificationId, request.type(),
                        attempt, e.getMessage());
            }
            return retry;
        }
    }

    /**
     * Espera {@code attempt × retryDelay} antes del siguiente intento.
     *
     * @return {@code false} si el hilo fue interrumpido durante la espera
     */
    private boolean pauseAfter(int attempt) {
        Duration delay = properties.retryDelay().multipliedBy(attempt);
        if (delay.isZero()) {
            return true;
        }
        try {
            Thread.sleep(delay);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
