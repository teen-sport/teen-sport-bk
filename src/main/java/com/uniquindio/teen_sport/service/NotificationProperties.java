package com.uniquindio.teen_sport.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Política de reintentos de las notificaciones ({@code app.notifications.*}).
 *
 * @param maxAttempts intentos de envío por notificación, al menos 1
 * @param retryDelay  espera base entre intentos; antes del intento {@code n + 1} se espera
 *                    {@code n × retryDelay}
 */
@ConfigurationProperties("app.notifications")
public record NotificationProperties(int maxAttempts, Duration retryDelay) {

    /**
     * @throws IllegalArgumentException si los valores no permiten enviar
     */
    public NotificationProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("app.notifications.max-attempts debe ser al menos 1");
        }
        if (retryDelay == null || retryDelay.isNegative()) {
            throw new IllegalArgumentException("app.notifications.retry-delay no puede ser negativo");
        }
    }
}
