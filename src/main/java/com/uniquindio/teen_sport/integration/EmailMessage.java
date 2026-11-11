package com.uniquindio.teen_sport.integration;

/**
 * Correo listo para enviar, independiente del proveedor.
 *
 * @param toEmail     dirección del destinatario
 * @param toName      nombre del destinatario
 * @param subject     asunto
 * @param htmlContent cuerpo en HTML, con los datos variables ya escapados
 */
public record EmailMessage(String toEmail, String toName, String subject, String htmlContent) {

    /** Evita volcar la dirección y el contenido en los logs. */
    @Override
    public String toString() {
        return "EmailMessage[subject=" + subject + "]";
    }
}
