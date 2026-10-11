package com.uniquindio.teen_sport.exception;

import org.springframework.http.HttpStatus;

/**
 * Error esperado de un caso de uso que se traduce a una respuesta HTTP estable.
 *
 * <p>Los servicios la lanzan con el código del contrato y el estado HTTP que el
 * contrato declara para ese caso (404, 409 o 422, entre otros). El mensaje debe
 * estar en español y no incluir datos internos ni personales.</p>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    /**
     * Crea la excepción.
     *
     * @param code    código estable del contrato
     * @param status  estado HTTP que corresponde al caso
     * @param message mensaje de presentación en español
     */
    public BusinessException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * @return código estable del contrato
     */
    public ErrorCode getCode() {
        return code;
    }

    /**
     * @return estado HTTP de la respuesta
     */
    public HttpStatus getStatus() {
        return status;
    }
}
