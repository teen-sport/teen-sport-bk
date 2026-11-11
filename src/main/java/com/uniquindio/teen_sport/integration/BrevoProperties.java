package com.uniquindio.teen_sport.integration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Brevo ({@code app.mail.brevo.*}). La clave llega por {@code BREVO_API_KEY} y nunca
 * se versiona.
 *
 * <p>Sin clave o sin remitente la aplicación arranca igual (por ejemplo, en desarrollo local), pero
 * cada notificación queda {@code FAILED}: no se afirma un envío que no ocurrió.</p>
 *
 * @param apiKey         clave de la API v3 de Brevo
 * @param senderEmail    remitente verificado en Brevo
 * @param senderName     nombre del remitente
 * @param baseUrl        URL base de la API
 * @param connectTimeout tiempo máximo para conectar
 * @param readTimeout    tiempo máximo de espera de la respuesta
 */
@ConfigurationProperties("app.mail.brevo")
public record BrevoProperties(String apiKey, String senderEmail, String senderName, String baseUrl,
                              Duration connectTimeout, Duration readTimeout) {

    /**
     * @return si hay clave y remitente para intentar envíos
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && senderEmail != null && !senderEmail.isBlank();
    }

    /** Evita volcar la clave en los logs. */
    @Override
    public String toString() {
        return "BrevoProperties[senderName=" + senderName + ", baseUrl=" + baseUrl + "]";
    }
}
