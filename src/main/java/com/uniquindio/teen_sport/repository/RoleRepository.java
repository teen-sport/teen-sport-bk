package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso al catálogo de roles.
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /**
     * @param name nombre del rol
     * @return el rol, si existe
     */
    Optional<Role> findByName(RoleName name);
}
