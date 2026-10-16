package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.SecurityMessages;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Regla de propiedad de los deportistas: el administrador accede a todos; un acudiente, solo a los
 * suyos. El acudiente se identifica siempre por el {@code guardianId} del token validado, nunca por
 * un valor enviado en la solicitud.
 */
@Component
public class AthleteAccessPolicy {

    /**
     * Acudiente al que se limita una consulta.
     *
     * @param user                identidad autenticada
     * @param requestedGuardianId filtro pedido por el cliente; solo se respeta para un administrador
     * @return el acudiente al que se limita la consulta, o {@code null} para no limitarla (administrador sin filtro)
     * @throws BusinessException 403 {@code ACCESS_DENIED} si el rol no puede consultar deportistas
     */
    public Integer guardianScope(AuthenticatedUser user, Integer requestedGuardianId) {
        if (user.role() == RoleName.ADMIN) {
            return requestedGuardianId;
        }
        return ownGuardianId(user);
    }

    /**
     * @param user    identidad autenticada
     * @param athlete deportista solicitado
     * @throws BusinessException 403 {@code ACCESS_DENIED} si el deportista no pertenece al acudiente
     */
    public void requireAccess(AuthenticatedUser user, Athlete athlete) {
        if (user.role() == RoleName.ADMIN) {
            return;
        }
        if (!ownGuardianId(user).equals(athlete.getGuardian().getId())) {
            throw accessDenied();
        }
    }

    /**
     * @param user identidad autenticada
     * @return el id de acudiente del token
     * @throws BusinessException 403 {@code ACCESS_DENIED} si la identidad no es de un acudiente
     */
    public Integer ownGuardianId(AuthenticatedUser user) {
        if (user.role() != RoleName.GUARDIAN || user.guardianId() == null) {
            throw accessDenied();
        }
        return user.guardianId();
    }

    private static BusinessException accessDenied() {
        return new BusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, SecurityMessages.ACCESS_DENIED);
    }
}
