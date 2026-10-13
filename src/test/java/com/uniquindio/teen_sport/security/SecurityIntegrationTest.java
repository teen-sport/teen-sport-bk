package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.exception.SecurityMessages;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.support.IntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticación JWT y CORS de la cadena de seguridad real (RNF-15, RNF-16).
 *
 * <p>Usa una ruta protegida inexistente: si la seguridad deja pasar la solicitud, la API
 * responde 404; si la rechaza, responde 401 antes de llegar al controlador.</p>
 */
@IntegrationTest
class SecurityIntegrationTest {

    private static final String PROTECTED = "/api/v1/protected-probe";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenService tokenService;
    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void requestWithoutTokenIsUnauthenticated() throws Exception {
        mockMvc.perform(get(PROTECTED))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value(SecurityMessages.UNAUTHENTICATED))
                .andExpect(jsonPath("$.path").value(PROTECTED));
    }

    @Test
    void malformedTokenIsUnauthenticated() throws Exception {
        withToken("esto-no-es-un-jwt")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void tokenWithTamperedSignatureIsUnauthenticated() throws Exception {
        String token = tokenService.issue(new AuthenticatedUser(1, RoleName.ADMIN, null, null));
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "firmaFalsa";

        withToken(tampered)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void expiredTokenIsReportedAsExpired() throws Exception {
        Instant past = Instant.now().minusSeconds(7200);
        String expired = encode(JwtClaimsSet.builder().subject("1").claim(TokenClaims.ROLE, "ADMIN")
                .issuedAt(past).expiresAt(past.plusSeconds(3600)).build());

        withToken(expired)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value(SecurityMessages.TOKEN_EXPIRED));
    }

    @Test
    void tokenWithUnknownRoleIsUnauthenticated() throws Exception {
        Instant now = Instant.now();
        String token = encode(JwtClaimsSet.builder().subject("1").claim(TokenClaims.ROLE, "SUPERADMIN")
                .issuedAt(now).expiresAt(now.plusSeconds(600)).build());

        withToken(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void tokenWithNonNumericSubjectIsUnauthenticated() throws Exception {
        Instant now = Instant.now();
        String token = encode(JwtClaimsSet.builder().subject("admin").claim(TokenClaims.ROLE, "ADMIN")
                .issuedAt(now).expiresAt(now.plusSeconds(600)).build());

        withToken(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void validTokenPassesTheSecurityFilter() throws Exception {
        withToken(tokenService.issue(new AuthenticatedUser(1, RoleName.COACH, null, 2)))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicEndpointsDoNotRequireToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void corsPreflightIsAllowedOnlyForConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://sitio-ajeno.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private ResultActions withToken(String token) throws Exception {
        return mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String encode(JwtClaimsSet claims) {
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
