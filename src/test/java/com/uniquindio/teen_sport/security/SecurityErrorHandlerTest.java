package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.dto.LoginRequest;
import com.uniquindio.teen_sport.dto.RegisterGuardianRequest;
import com.uniquindio.teen_sport.exception.ApiErrorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Respuestas de seguridad que aún no ejercita ninguna ruta (403 por rol, que se usará desde
 * HU30) y protecciones de datos sensibles en los registros.
 */
class SecurityErrorHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    private final SecurityErrorHandler handler = new SecurityErrorHandler(
            new ApiErrorFactory(Clock.fixed(NOW, ZoneOffset.UTC)), JsonMapper.builder().build());

    @Test
    void accessDeniedIsForbiddenWithContractFormat() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("rol insuficiente"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"ACCESS_DENIED\"")
                .contains("\"path\":\"/api/v1/users\"")
                .contains(SecurityErrorHandler.ACCESS_DENIED_MESSAGE)
                .doesNotContain("rol insuficiente");
    }

    @Test
    void tokenWithoutExpirationIsRejected() {
        Jwt jwt = new Jwt("token", NOW, null, Map.of("alg", "HS256"), Map.of("sub", "1"));

        assertThat(new JwtExpiryValidator(Clock.fixed(NOW, ZoneOffset.UTC)).validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void requestDtosNeverPrintThePassword() {
        assertThat(new LoginRequest("ana@teensport.test", "Segura#2026").toString()).doesNotContain("Segura#2026");
        assertThat(new RegisterGuardianRequest("Ana", "Prueba", "CC", "1", "ana@teensport.test", "300", null,
                "Segura#2026", true).toString()).doesNotContain("Segura#2026");
    }
}
