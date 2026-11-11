package com.uniquindio.teen_sport.integration;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptador de Brevo (ADR-09): envía correos transaccionales con {@code POST /v3/smtp/email}.
 *
 * <p>Se usa la API REST con {@link RestClient} en lugar del SDK de Brevo (decisión del 2026-11-10):
 * solo se necesita esta operación, no añade dependencias y evita las librerías antiguas que arrastra
 * el SDK. A Brevo solo se envían el destinatario, el asunto y el cuerpo.</p>
 *
 * <p>Son transitorios, y por tanto reintentables, los fallos de red, {@code 429} y los {@code 5xx}. Los
 * demás {@code 4xx} (clave inválida, remitente no verificado, datos inválidos) son definitivos.</p>
 */
@Component
public class BrevoEmailSender implements EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoEmailSender.class);
    private static final String SEND_PATH = "/smtp/email";
    private static final String API_KEY_HEADER = "api-key";

    private final RestClient restClient;
    private final BrevoProperties properties;

    /**
     * @param brevoRestClient cliente HTTP de Brevo
     * @param properties      clave y remitente
     */
    public BrevoEmailSender(RestClient brevoRestClient, BrevoProperties properties) {
        this.restClient = brevoRestClient;
        this.properties = properties;
        if (!properties.isConfigured()) {
            LOG.warn("Brevo no está configurado (BREVO_API_KEY o MAIL_SENDER_EMAIL vacíos): "
                    + "las notificaciones quedarán como FAILED");
        }
    }

    @Override
    public void send(EmailMessage message) {
        if (!properties.isConfigured()) {
            throw new EmailDeliveryException("El proveedor de correo no está configurado", false);
        }
        SendRequest request = new SendRequest(new Contact(properties.senderEmail(), properties.senderName()),
                List.of(new Contact(message.toEmail(), message.toName())), message.subject(), message.htmlContent());
        try {
            restClient.post()
                    .uri(SEND_PATH)
                    .header(API_KEY_HEADER, properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            throw new EmailDeliveryException("Brevo rechazó el correo con estado " + status.value(),
                    isTransient(status), e);
        } catch (ResourceAccessException e) {
            throw new EmailDeliveryException("No fue posible comunicarse con Brevo", true, e);
        }
    }

    private static boolean isTransient(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS) || status.is5xxServerError();
    }

    /**
     * Cuerpo de {@code POST /v3/smtp/email}.
     *
     * @param sender      remitente
     * @param to          destinatarios
     * @param subject     asunto
     * @param htmlContent cuerpo en HTML
     */
    record SendRequest(Contact sender, List<Contact> to, String subject, String htmlContent) {
    }

    /**
     * @param email dirección
     * @param name  nombre visible
     */
    record Contact(String email, String name) {
    }
}
