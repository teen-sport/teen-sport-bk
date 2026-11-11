package com.uniquindio.teen_sport.support;

import com.uniquindio.teen_sport.integration.EmailDeliveryException;
import com.uniquindio.teen_sport.integration.EmailMessage;
import com.uniquindio.teen_sport.integration.EmailSender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Proveedor de correo de las pruebas de integración: guarda los mensajes en memoria y nunca llama a
 * Brevo. Puede configurarse para fallar con los destinatarios indicados, sin afectar a las pruebas que
 * usan otras direcciones.
 */
public class RecordingEmailSender implements EmailSender {

    private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    private volatile Predicate<String> failingRecipients = recipient -> false;
    private volatile boolean retryableFailure;

    @Override
    public void send(EmailMessage message) {
        if (failingRecipients.test(message.toEmail())) {
            throw new EmailDeliveryException("Fallo simulado del proveedor", retryableFailure);
        }
        sent.add(message);
    }

    /**
     * @param email destinatario
     * @return los mensajes aceptados para ese destinatario, en orden de envío
     */
    public List<EmailMessage> sentTo(String email) {
        return sent.stream().filter(message -> message.toEmail().equals(email)).toList();
    }

    /**
     * Hace fallar los envíos a un destinatario.
     *
     * @param email     destinatario
     * @param retryable si el fallo simulado es transitorio
     */
    public void failFor(String email, boolean retryable) {
        this.retryableFailure = retryable;
        this.failingRecipients = email::equals;
    }

    /** Vuelve a aceptar todos los envíos. */
    public void acceptAll() {
        this.failingRecipients = recipient -> false;
    }
}
