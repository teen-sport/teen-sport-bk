package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.entity.RoleName;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Identidad del usuario de la solicitud en curso, tomada del JWT ya validado por Spring Security.
 *
 * <p>Es la única fuente de identidad para las operaciones "propias" (por ejemplo, {@code /users/me}):
 * nunca se usan ids, roles o correos enviados en el cuerpo o en la URL para decidir de quién son
 * los datos.</p>
 */
@Component
public class CurrentUserService {

    /**
     * @return identidad del usuario autenticado
     * @throws IllegalStateException si se invoca en una ruta sin autenticación JWT, lo que indica un
     *                               error de configuración de seguridad
     */
    public AuthenticatedUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("La solicitud no está autenticada con un JWT");
        }
        Jwt jwt = token.getToken();
        return new AuthenticatedUser(
                Integer.valueOf(jwt.getSubject()),
                RoleName.valueOf(jwt.getClaimAsString(TokenClaims.ROLE)),
                integerClaim(jwt, TokenClaims.GUARDIAN_ID),
                integerClaim(jwt, TokenClaims.COACH_ID));
    }

    private static Integer integerClaim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        return value instanceof Number number ? number.intValue() : null;
    }
}
