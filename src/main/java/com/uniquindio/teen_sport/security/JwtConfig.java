package com.uniquindio.teen_sport.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.uniquindio.teen_sport.entity.RoleName;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Codificador y decodificador de JWT firmados con HS256 (ADR-007).
 *
 * <p>El decodificador solo acepta HS256 con el secreto configurado (firma), exige {@code exp}
 * vigente según el reloj de la aplicación y comprueba que {@code sub} sea un id de usuario
 * numérico y que {@code role} sea uno de los roles del contrato.</p>
 */
@Configuration
public class JwtConfig {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Pattern USER_ID = Pattern.compile("[0-9]{1,9}");

    /**
     * @param properties configuración JWT validada
     * @return clave HMAC derivada del secreto
     */
    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    /**
     * @param jwtSecretKey clave HMAC
     * @return codificador que firma los tokens emitidos en el login
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * @param jwtSecretKey clave HMAC
     * @param clock        reloj de la aplicación
     * @return decodificador que valida firma, algoritmo, vigencia y claims de cada solicitud
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(tokenValidator(clock));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> tokenValidator(Clock clock) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtExpiryValidator(clock),
                new JwtClaimValidator<>(JwtClaimNames.SUB, JwtConfig::isUserId),
                new JwtClaimValidator<>(TokenClaims.ROLE, JwtConfig::isKnownRole));
    }

    private static boolean isUserId(Object subject) {
        return subject instanceof String id && USER_ID.matcher(id).matches();
    }

    private static boolean isKnownRole(Object role) {
        return role instanceof String name
                && Arrays.stream(RoleName.values()).anyMatch(known -> known.name().equals(name));
    }
}
