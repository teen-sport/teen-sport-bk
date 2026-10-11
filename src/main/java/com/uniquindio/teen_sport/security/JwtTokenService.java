package com.uniquindio.teen_sport.security;

import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite los JWT de acceso con los claims del contrato: {@code sub}, {@code role},
 * {@code guardianId} o {@code coachId}, {@code iat} y {@code exp}.
 *
 * <p>El token no lleva datos personales (correo, nombres ni documento).</p>
 */
@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    /**
     * @param encoder    codificador HS256
     * @param properties vigencia de los tokens
     * @param clock      reloj de la aplicación
     */
    public JwtTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Emite un token para la identidad indicada, vigente durante {@link #expirationSeconds()}.
     *
     * @param user identidad autenticada
     * @return JWT firmado y serializado
     */
    public String issue(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.userId()))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(properties.expirationSeconds()))
                .claim(TokenClaims.ROLE, user.role().name());
        if (user.guardianId() != null) {
            claims.claim(TokenClaims.GUARDIAN_ID, user.guardianId());
        }
        if (user.coachId() != null) {
            claims.claim(TokenClaims.COACH_ID, user.coachId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    /**
     * @return vigencia de los tokens en segundos
     */
    public long expirationSeconds() {
        return properties.expirationSeconds();
    }
}
