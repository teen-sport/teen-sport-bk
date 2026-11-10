package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.RoleName;

/**
 * Número de cuentas de un rol en un estado (R5).
 */
public interface RoleStatusCount {

    /**
     * @return rol de las cuentas
     */
    RoleName getRole();

    /**
     * @return estado actual de las cuentas
     */
    EntityStatus getStatus();

    /**
     * @return cuentas de ese rol en ese estado
     */
    Long getTotal();
}
