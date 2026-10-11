package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.entity.RoleName;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Emisión y validación de JWT con reloj controlado (RNF-16).
 */
class JwtTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef-test";
    private static final Instant ISSUED_AT = Instant.parse("2026-10-11T12:00:00Z");
    private static final long EXPIRATION = 3600;

    private final JwtConfig config = new JwtConfig();
    private final SecretKey key = config.jwtSecretKey(new JwtProperties(SECRET, EXPIRATION));
    private final JwtTokenService service = new JwtTokenService(config.jwtEncoder(key),
            new JwtProperties(SECRET, EXPIRATION), Clock.fixed(ISSUED_AT, ZoneOffset.UTC));

    @Test
    void guardianTokenCarriesContractClaimsAndNoPersonalData() {
        String token = service.issue(new AuthenticatedUser(7, RoleName.GUARDIAN, 3, null));

        Jwt jwt = decoderAt(ISSUED_AT).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString(TokenClaims.ROLE)).isEqualTo("GUARDIAN");
        assertThat(((Number) jwt.getClaim(TokenClaims.GUARDIAN_ID)).intValue()).isEqualTo(3);
        assertThat(jwt.hasClaim(TokenClaims.COACH_ID)).isFalse();
        assertThat(jwt.getIssuedAt()).isEqualTo(ISSUED_AT);
        assertThat(jwt.getExpiresAt()).isEqualTo(ISSUED_AT.plusSeconds(EXPIRATION));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getClaims()).containsOnlyKeys("sub", "role", "guardianId", "iat", "exp");
        assertThat(service.expirationSeconds()).isEqualTo(EXPIRATION);
    }

    @Test
    void coachTokenCarriesCoachId() {
        Jwt jwt = decoderAt(ISSUED_AT).decode(service.issue(new AuthenticatedUser(8, RoleName.COACH, null, 5)));

        assertThat(((Number) jwt.getClaim(TokenClaims.COACH_ID)).intValue()).isEqualTo(5);
        assertThat(jwt.hasClaim(TokenClaims.GUARDIAN_ID)).isFalse();
    }

    @Test
    void tokenIsValidOneSecondBeforeExpiration() {
        String token = service.issue(new AuthenticatedUser(1, RoleName.ADMIN, null, null));

        assertThat(decoderAt(ISSUED_AT.plusSeconds(EXPIRATION - 1)).decode(token).getSubject()).isEqualTo("1");
    }

    @Test
    void tokenIsRejectedAsExpiredExactlyAtExpirationAndAfter() {
        String token = service.issue(new AuthenticatedUser(1, RoleName.ADMIN, null, null));

        for (long secondsAfterIssue : new long[]{EXPIRATION, EXPIRATION + 1}) {
            assertThatThrownBy(() -> decoderAt(ISSUED_AT.plusSeconds(secondsAfterIssue)).decode(token))
                    .isInstanceOfSatisfying(JwtValidationException.class, ex -> assertThat(ex.getErrors())
                            .anyMatch(error -> JwtExpiryValidator.TOKEN_EXPIRED.equals(error.getErrorCode())));
        }
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtConfig otherConfig = new JwtConfig();
        SecretKey otherKey = otherConfig.jwtSecretKey(new JwtProperties("another-secret-with-more-than-32-bytes", 60));
        JwtTokenService forger = new JwtTokenService(otherConfig.jwtEncoder(otherKey),
                new JwtProperties("another-secret-with-more-than-32-bytes", 60), Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
        String forged = forger.issue(new AuthenticatedUser(1, RoleName.ADMIN, null, null));

        assertThatThrownBy(() -> decoderAt(ISSUED_AT).decode(forged)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> decoderAt(ISSUED_AT).decode("not-a-jwt")).isInstanceOf(BadJwtException.class);
    }

    private JwtDecoder decoderAt(Instant now) {
        return config.jwtDecoder(key, Clock.fixed(now, ZoneOffset.UTC));
    }
}
