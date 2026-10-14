package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Estados de la matrícula que admite cada operación: la revisión (E12, E13) solo sobre una matrícula
 * pendiente; la cancelación, el cambio de grupo y los pagos (E14–E17), sobre una vigente.
 */
public final class EnrollmentStatePolicy {

    static final String NOT_PENDING_MESSAGE = "Solo se puede revisar una matrícula pendiente de revisión.";
    static final String FINISHED_MESSAGE = "La matrícula ya está rechazada o cancelada.";

    private EnrollmentStatePolicy() {
    }

    /**
     * @param enrollment matrícula
     * @throws BusinessException 409 {@code INVALID_STATE} si no está {@code PENDING_REVIEW}
     */
    public static void requirePending(Enrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.PENDING_REVIEW) {
            throw invalidState(NOT_PENDING_MESSAGE);
        }
    }

    /**
     * @param enrollment matrícula
     * @throws BusinessException 409 {@code INVALID_STATE} si está rechazada o cancelada
     */
    public static void requireInForce(Enrollment enrollment) {
        if (!EnrollmentStatus.IN_FORCE.contains(enrollment.getStatus())) {
            throw invalidState(FINISHED_MESSAGE);
        }
    }

    private static BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_STATE, HttpStatus.CONFLICT, message);
    }
}
