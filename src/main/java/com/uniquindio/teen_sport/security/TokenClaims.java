package com.uniquindio.teen_sport.security;

/**
 * Nombres de los claims propios del JWT de TeenSport (además de {@code sub}, {@code iat} y {@code exp}).
 */
public final class TokenClaims {

    /** Rol del usuario: {@code ADMIN}, {@code COACH} o {@code GUARDIAN}. */
    public static final String ROLE = "role";

    /** Id del perfil de acudiente. */
    public static final String GUARDIAN_ID = "guardianId";

    /** Id del perfil de entrenador. */
    public static final String COACH_ID = "coachId";

    private TokenClaims() {
    }
}
