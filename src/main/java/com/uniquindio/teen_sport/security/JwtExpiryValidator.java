package com.uniquindio.teen_sport.security;

import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza los JWT sin {@code exp} o cuyo {@code exp} ya llegó, según el reloj de la aplicación.
 *
 * <p>Reemplaza al validador por defecto de Spring Security por dos motivos: no aplica
 * tolerancia de reloj (el token deja de valer exactamente en {@code exp}, RNF-16) y marca el
 * fallo con el código {@value #TOKEN_EXPIRED}, que permite responder {@code TOKEN_EXPIRED} en
 * lugar de {@code UNAUTHENTICATED}.</p>
 */
public class JwtExpiryValidator implements OAuth2TokenValidator<Jwt> {

    /** Código interno del error de validación de un token vencido. */
    public static final String TOKEN_EXPIRED = "token_expired";

    private static final OAuth2Error EXPIRED = new OAuth2Error(TOKEN_EXPIRED, "El token expiró", null);
    private static final OAuth2Error MISSING_EXPIRATION =
            new OAuth2Error("invalid_token", "El token no tiene fecha de expiración", null);

    private final Clock clock;

    /**
     * @param clock reloj de la aplicación
     */
    public JwtExpiryValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return OAuth2TokenValidatorResult.failure(MISSING_EXPIRATION);
        }
        if (!clock.instant().isBefore(expiresAt)) {
            return OAuth2TokenValidatorResult.failure(EXPIRED);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
