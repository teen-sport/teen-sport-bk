package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Reglas del grupo que recibe una matrícula (E6, E12 y E15).
 */
public final class GroupEnrollmentPolicy {

    static final String GROUP_INACTIVE_MESSAGE = "El grupo no está disponible para matrículas.";
    static final String OTHER_DISCIPLINE_MESSAGE = "El grupo debe ser de la misma disciplina de la matrícula.";

    private GroupEnrollmentPolicy() {
    }

    /**
     * @param group grupo con su disciplina
     * @throws BusinessException 422 {@code GROUP_INACTIVE} si el grupo o su disciplina están inactivos
     */
    public static void requireOpen(Group group) {
        if (group.getStatus() != EntityStatus.ACTIVE || group.getDiscipline().getStatus() != EntityStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.GROUP_INACTIVE, HttpStatus.UNPROCESSABLE_CONTENT,
                    GROUP_INACTIVE_MESSAGE);
        }
    }

    /**
     * @param current grupo actual de la matrícula
     * @param target  grupo al que se quiere mover
     * @throws BusinessException 422 {@code GROUP_OTHER_DISCIPLINE} si son de disciplinas distintas
     */
    public static void requireSameDiscipline(Group current, Group target) {
        if (!current.getDiscipline().getId().equals(target.getDiscipline().getId())) {
            throw new BusinessException(ErrorCode.GROUP_OTHER_DISCIPLINE, HttpStatus.UNPROCESSABLE_CONTENT,
                    OTHER_DISCIPLINE_MESSAGE);
        }
    }
}
