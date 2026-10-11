package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Coach;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a los perfiles de entrenador.
 */
public interface CoachRepository extends JpaRepository<Coach, Integer> {

    /**
     * @param userId id de la cuenta
     * @return el perfil de entrenador de esa cuenta, si existe
     */
    Optional<Coach> findByUserId(Integer userId);
}
