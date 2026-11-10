package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.uniquindio.teen_sport.entity.RoleName;
import java.time.YearMonth;
import java.util.List;

/**
 * Indicadores de cuentas de usuario (R5), con los KPI del Proceso 3.
 *
 * @param period                mes de creación de las cuentas contadas en {@code registeredInPeriod}
 * @param registeredInPeriod    cuentas creadas en el periodo, de todos los roles
 * @param inactiveAccounts      cuentas inactivas hoy, de todos los roles
 * @param guardiansWithAthletes acudientes activos con al menos un deportista vinculado
 * @param guardianLinkRate      porcentaje de {@code guardiansWithAthletes} sobre los acudientes activos
 * @param byRole                desglose por rol; incluye los tres roles, aunque sus valores sean cero
 */
public record UserReport(@JsonFormat(pattern = "yyyy-MM") YearMonth period, long registeredInPeriod,
                         long inactiveAccounts, long guardiansWithAthletes, double guardianLinkRate,
                         List<RoleAccounts> byRole) {

    /**
     * @param role               rol
     * @param registeredInPeriod cuentas del rol creadas en el periodo
     * @param active             cuentas del rol activas hoy
     * @param inactive           cuentas del rol inactivas hoy
     */
    public record RoleAccounts(RoleName role, long registeredInPeriod, long active, long inactive) {
    }
}
