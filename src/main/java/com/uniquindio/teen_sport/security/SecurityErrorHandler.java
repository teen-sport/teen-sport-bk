package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.exception.ApiError;
import com.uniquindio.teen_sport.exception.ApiErrorFactory;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.SecurityMessages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde los rechazos del filtro de seguridad con el formato {@link ApiError} del contrato.
 *
 * <ul>
 *   <li>401 {@code TOKEN_EXPIRED}: el token llegó vencido.</li>
 *   <li>401 {@code UNAUTHENTICATED}: falta el token, o es inválido, malformado o con firma incorrecta.</li>
 *   <li>403 {@code ACCESS_DENIED}: el usuario autenticado no tiene el rol requerido.</li>
 * </ul>
 *
 * <p>No incluye el motivo técnico del rechazo en la respuesta.</p>
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {


    private final ApiErrorFactory errorFactory;
    private final JsonMapper jsonMapper;

    /**
     * @param errorFactory constructor del cuerpo de error
     * @param jsonMapper   serializador JSON de la aplicación
     */
    public SecurityErrorHandler(ApiErrorFactory errorFactory, JsonMapper jsonMapper) {
        this.errorFactory = errorFactory;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        boolean expired = isExpiredToken(exception);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, errorFactory.create(HttpStatus.UNAUTHORIZED,
                expired ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHENTICATED,
                expired ? SecurityMessages.TOKEN_EXPIRED : SecurityMessages.UNAUTHENTICATED,
                request.getRequestURI()));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, errorFactory.create(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, SecurityMessages.ACCESS_DENIED,
                request.getRequestURI()));
    }

    /**
     * Spring Security envuelve el fallo de validación del JWT; se recorre la cadena de causas
     * para encontrar el error marcado por {@link JwtExpiryValidator}.
     */
    private static boolean isExpiredToken(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof JwtValidationException validation) {
                return validation.getErrors().stream()
                        .anyMatch(error -> JwtExpiryValidator.TOKEN_EXPIRED.equals(error.getErrorCode()));
            }
        }
        return false;
    }

    private void write(HttpServletResponse response, ApiError body) throws IOException {
        response.setStatus(body.status());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
