package com.uniquindio.teen_sport.integration;

/**
 * El proveedor de correo no aceptó un mensaje.
 *
 * <p>{@link #isRetryable()} distingue los fallos transitorios (red, límite de peticiones, error del
 * proveedor), que vale la pena reintentar, de los definitivos (credenciales, datos inválidos), que
 * fallarían igual en cada intento.</p>
 */
public class EmailDeliveryException extends RuntimeException {

    private final boolean retryable;

    /**
     * @param message   descripción técnica, sin datos personales
     * @param retryable si un nuevo intento podría tener éxito
     */
    public EmailDeliveryException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    /**
     * @param message   descripción técnica, sin datos personales
     * @param retryable si un nuevo intento podría tener éxito
     * @param cause     error original
     */
    public EmailDeliveryException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
