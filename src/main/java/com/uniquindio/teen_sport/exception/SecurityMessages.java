package com.uniquindio.teen_sport.exception;

/**
 * Mensajes en español de los rechazos de autenticación y autorización.
 *
 * <p>Los comparten el filtro de seguridad, el manejador global de excepciones y los servicios,
 * para que el mismo rechazo tenga siempre el mismo texto.</p>
 */
public final class SecurityMessages {

    /** Solicitud sin una identidad válida (401 {@code UNAUTHENTICATED}). */
    public static final String UNAUTHENTICATED = "Debes iniciar sesión para continuar.";

    /** Token vencido (401 {@code TOKEN_EXPIRED}). */
    public static final String TOKEN_EXPIRED = "Tu sesión expiró. Inicia sesión de nuevo.";

    /** Usuario autenticado sin permiso sobre la operación (403 {@code ACCESS_DENIED}). */
    public static final String ACCESS_DENIED = "No tienes permiso para realizar esta acción.";

    private SecurityMessages() {
    }
}
