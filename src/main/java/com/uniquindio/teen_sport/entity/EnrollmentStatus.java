package com.uniquindio.teen_sport.entity;

import java.util.Set;

/**
 * Estado de una matrícula (esquema {@code EnrollmentStatus} del contrato).
 *
 * <p>{@code REJECTED} y {@code CANCELLED} son estados finales.</p>
 */
public enum EnrollmentStatus {
    PENDING_REVIEW,
    ACTIVE,
    REJECTED,
    CANCELLED;

    /** Estados de una matrícula vigente: ocupa cupo y bloquea otra matrícula en la misma disciplina. */
    public static final Set<EnrollmentStatus> IN_FORCE = Set.of(PENDING_REVIEW, ACTIVE);
}
