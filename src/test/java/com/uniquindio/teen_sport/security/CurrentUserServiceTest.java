package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.entity.RoleName;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsIdentityFromTheValidatedJwt() {
        authenticate(Map.of("sub", "7", TokenClaims.ROLE, "GUARDIAN", TokenClaims.GUARDIAN_ID, 3L));

        assertThat(service.current()).isEqualTo(new AuthenticatedUser(7, RoleName.GUARDIAN, 3, null));
    }

    @Test
    void readsCoachId() {
        authenticate(Map.of("sub", "8", TokenClaims.ROLE, "COACH", TokenClaims.COACH_ID, 5));

        assertThat(service.current()).isEqualTo(new AuthenticatedUser(8, RoleName.COACH, null, 5));
    }

    @Test
    void failsWhenTheRequestIsNotAuthenticatedWithJwt() {
        assertThatThrownBy(service::current).isInstanceOf(IllegalStateException.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "secret"));
        assertThatThrownBy(service::current).isInstanceOf(IllegalStateException.class);
    }

    private static void authenticate(Map<String, Object> claims) {
        Instant now = Instant.now();
        Jwt jwt = new Jwt("token", now, now.plusSeconds(60), Map.of("alg", "HS256"), claims);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
