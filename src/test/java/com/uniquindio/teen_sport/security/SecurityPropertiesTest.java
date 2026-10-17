package com.uniquindio.teen_sport.security;

import com.uniquindio.teen_sport.config.AdminBootstrapProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validaciones de arranque de la configuración sensible: la aplicación no debe iniciar con un
 * secreto débil, CORS abierto o un administrador inicial a medio configurar.
 */
class SecurityPropertiesTest {

    private static final String SECRET_32_BYTES = "0123456789abcdef0123456789abcdef";

    @Test
    void jwtSecretMustHaveAtLeast32Bytes() {
        assertThatThrownBy(() -> new JwtProperties("short-secret", 3600))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(null, 3600)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new JwtProperties(SECRET_32_BYTES, 3600).expirationSeconds()).isEqualTo(3600);
    }

    @Test
    void jwtExpirationMustBePositive() {
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void corsRejectsEmptyListAndWildcards() {
        assertThatThrownBy(() -> new CorsProperties(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of("*"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new CorsProperties(List.of("http://localhost:4200")).allowedOrigins())
                .containsExactly("http://localhost:4200");
    }

    @Test
    void adminBootstrapRequiresAllMandatoryValuesOrNone() {
        assertThat(admin("", "", "").isConfigured()).isFalse();
        assertThat(admin("admin@teensport.test", "Admin#2026", "123").isConfigured()).isTrue();
        AdminBootstrapProperties partial = admin("admin@teensport.test", "", "");
        assertThatThrownBy(partial::isConfigured).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adminBootstrapToStringDoesNotExposePassword() {
        assertThat(admin("admin@teensport.test", "Admin#2026", "123").toString()).doesNotContain("Admin#2026");
    }

    private static AdminBootstrapProperties admin(String email, String password, String idNumber) {
        return new AdminBootstrapProperties(email, password, idNumber, "CC", "Administrador", "TeenSport");
    }
}
