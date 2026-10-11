package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Guardian;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a los perfiles de acudiente.
 */
public interface GuardianRepository extends JpaRepository<Guardian, Integer> {

    /**
     * @param userId id de la cuenta
     * @return el perfil de acudiente de esa cuenta, si existe
     */
    Optional<Guardian> findByUserId(Integer userId);
}
