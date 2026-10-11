package com.uniquindio.teen_sport.exception;

import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Construye el cuerpo {@link ApiError} con la hora del reloj de la aplicación.
 *
 * <p>Lo comparten el manejador global de excepciones y los manejadores de seguridad
 * (401/403), que se ejecutan fuera de Spring MVC, para que todas las respuestas de error
 * tengan exactamente el mismo formato.</p>
 */
@Component
public class ApiErrorFactory {

    private final Clock clock;

    /**
     * @param clock reloj de la aplicación
     */
    public ApiErrorFactory(Clock clock) {
        this.clock = clock;
    }

    /**
     * Crea un error sin detalles de campos.
     *
     * @param status  estado HTTP
     * @param code    código estable del contrato
     * @param message mensaje en español
     * @param path    ruta de la solicitud
     * @return cuerpo del error
     */
    public ApiError create(HttpStatus status, ErrorCode code, String message, String path) {
        return create(status, code, message, List.of(), path);
    }

    /**
     * Crea un error con detalles de campos.
     *
     * @param status  estado HTTP
     * @param code    código estable del contrato
     * @param message mensaje en español
     * @param details campos rechazados
     * @param path    ruta de la solicitud
     * @return cuerpo del error
     */
    public ApiError create(HttpStatus status, ErrorCode code, String message, List<ApiErrorDetail> details,
                           String path) {
        return new ApiError(clock.instant(), status.value(), code, message, details, path);
    }
}
