package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.RoleName;

/**
 * Número de cuentas de un rol (R5).
 */
public interface RoleCount {

    /**
     * @return rol de las cuentas
     */
    RoleName getRole();

    /**
     * @return cuentas de ese rol
     */
    Long getTotal();
}
