package com.uniquindio.teen_sport.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones de la capa web y de los casos de uso al formato {@link ApiError}.
 *
 * <p>Nunca expone trazas, SQL ni detalles internos: solo el código del contrato y un
 * mensaje en español. Los errores 401/403 de seguridad se incorporan con Spring Security
 * en la Fase 1.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String INVALID_FIELDS_MESSAGE = "La solicitud contiene campos inválidos.";
    static final String UNREADABLE_BODY_MESSAGE = "El cuerpo de la solicitud no tiene un formato válido.";
    static final String RESOURCE_NOT_FOUND_MESSAGE = "El recurso solicitado no existe.";

    private final Clock clock;

    /**
     * Crea el manejador con el reloj del sistema en UTC.
     */
    public GlobalExceptionHandler() {
        this(Clock.systemUTC());
    }

    GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Errores esperados de los casos de uso.
     *
     * @param ex      excepción de negocio
     * @param request solicitud en curso
     * @return respuesta con el estado y el código definidos por la excepción
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), List.of(), request);
    }

    /**
     * Cuerpo JSON que no cumple las restricciones de Bean Validation.
     *
     * @param ex      excepción de validación del cuerpo
     * @param request solicitud en curso
     * @return 400 {@code VALIDATION_ERROR} con un detalle por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return validationError(details, request);
    }

    /**
     * Parámetros de ruta o de consulta que no cumplen sus restricciones.
     *
     * @param ex      excepción de validación del método
     * @param request solicitud en curso
     * @return 400 {@code VALIDATION_ERROR} con un detalle por parámetro
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleInvalidParameters(HandlerMethodValidationException ex,
                                                            HttpServletRequest request) {
        List<ApiErrorDetail> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiErrorDetail(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return validationError(details, request);
    }

    /**
     * Parámetro con un tipo que no se puede convertir (por ejemplo, texto en un id numérico).
     *
     * @param ex      excepción de conversión
     * @param request solicitud en curso
     * @return 400 {@code VALIDATION_ERROR} indicando el parámetro
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return validationError(
                List.of(new ApiErrorDetail(ex.getName(), "El valor no tiene un formato válido.")), request);
    }

    /**
     * Parámetro de consulta obligatorio ausente.
     *
     * @param ex      excepción de parámetro faltante
     * @param request solicitud en curso
     * @return 400 {@code VALIDATION_ERROR} indicando el parámetro
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        return validationError(
                List.of(new ApiErrorDetail(ex.getParameterName(), "El parámetro es obligatorio.")), request);
    }

    /**
     * Cuerpo ausente o JSON mal formado.
     *
     * @param ex      excepción de lectura del cuerpo
     * @param request solicitud en curso
     * @return 400 {@code VALIDATION_ERROR} sin detalles internos del parser
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, UNREADABLE_BODY_MESSAGE,
                List.of(), request);
    }

    /**
     * Ruta que no corresponde a ningún endpoint.
     *
     * @param ex      excepción de recurso inexistente
     * @param request solicitud en curso
     * @return 404 {@code NOT_FOUND}
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, RESOURCE_NOT_FOUND_MESSAGE, List.of(), request);
    }

    private ResponseEntity<ApiError> validationError(List<ApiErrorDetail> details, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, INVALID_FIELDS_MESSAGE, details, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message,
                                           List<ApiErrorDetail> details, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(clock), status.value(), code, message, details,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
