package com.uniquindio.teen_sport.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
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
 * mensaje en español. Los fallos no previstos responden 500 {@code INTERNAL_ERROR} y su
 * causa solo se registra en el log. Los errores 401/403 de autenticación y autorización
 * del filtro de seguridad los responde {@code SecurityErrorHandler} con el mismo formato.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String INVALID_FIELDS_MESSAGE = "La solicitud contiene campos inválidos.";
    static final String UNREADABLE_BODY_MESSAGE = "El cuerpo de la solicitud no tiene un formato válido.";
    static final String RESOURCE_NOT_FOUND_MESSAGE = "El recurso solicitado no existe.";
    static final String UNEXPECTED_ERROR_MESSAGE = "Ocurrió un error inesperado. Intenta de nuevo más tarde.";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiErrorFactory errorFactory;

    /**
     * @param errorFactory constructor del cuerpo de error
     */
    public GlobalExceptionHandler(ApiErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
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

    /**
     * Rol insuficiente detectado por {@code @PreAuthorize} dentro de Spring MVC. Sin este manejador
     * caería en {@link #handleUnexpected} y respondería 500.
     *
     * @param ex      rechazo de autorización
     * @param request solicitud en curso
     * @return 403 {@code ACCESS_DENIED}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, SecurityMessages.ACCESS_DENIED,
                List.of(), request);
    }

    /**
     * Fallo no previsto por ningún otro manejador.
     *
     * <p>Las excepciones propias de Spring MVC que ya declaran su estado HTTP
     * ({@link ErrorResponse}, por ejemplo 405 o 415) se relanzan sin cambios para que Spring
     * las resuelva con ese estado en lugar de convertirlas en un 500. El resto se registra
     * con su traza en el log y responde un mensaje genérico.</p>
     *
     * @param ex      excepción no controlada
     * @param request solicitud en curso
     * @return 500 {@code INTERNAL_ERROR} sin detalles internos
     * @throws Exception la misma excepción cuando es un {@link ErrorResponse} de Spring MVC
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) throws Exception {
        if (ex instanceof ErrorResponse) {
            throw ex;
        }
        LOGGER.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, UNEXPECTED_ERROR_MESSAGE,
                List.of(), request);
    }

    private ResponseEntity<ApiError> validationError(List<ApiErrorDetail> details, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, INVALID_FIELDS_MESSAGE, details, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message,
                                           List<ApiErrorDetail> details, HttpServletRequest request) {
        ApiError body = errorFactory.create(status, code, message, details, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
