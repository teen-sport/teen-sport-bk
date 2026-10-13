package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.SecurityMessages;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Regla de propiedad de los grupos: el administrador accede a todos; un entrenador, solo a los que
 * tiene a cargo. El entrenador se identifica por el {@code coachId} del token validado.
 */
@Component
public class GroupAccessPolicy {

    /**
     * @param user             identidad autenticada
     * @param requestedCoachId filtro pedido por el cliente; solo se respeta para un administrador
     * @return el entrenador al que se limita la consulta, o {@code null} para no limitarla
     * @throws BusinessException 403 {@code ACCESS_DENIED} si el rol no puede consultar grupos
     */
    public Integer coachScope(AuthenticatedUser user, Integer requestedCoachId) {
        return user.role() == RoleName.ADMIN ? requestedCoachId : ownCoachId(user);
    }

    /**
     * @param user  identidad autenticada
     * @param group grupo solicitado
     * @throws BusinessException 403 {@code ACCESS_DENIED} si el grupo no está a cargo del entrenador
     */
    public void requireAccess(AuthenticatedUser user, Group group) {
        if (user.role() != RoleName.ADMIN && !ownCoachId(user).equals(group.getCoach().getId())) {
            throw accessDenied();
        }
    }

    private static Integer ownCoachId(AuthenticatedUser user) {
        if (user.role() != RoleName.COACH || user.coachId() == null) {
            throw accessDenied();
        }
        return user.coachId();
    }

    private static BusinessException accessDenied() {
        return new BusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, SecurityMessages.ACCESS_DENIED);
    }
}
