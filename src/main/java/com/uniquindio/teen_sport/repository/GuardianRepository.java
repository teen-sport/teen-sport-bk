package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Guardian;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso a los perfiles de acudiente.
 */
public interface GuardianRepository extends JpaRepository<Guardian, Integer>, JpaSpecificationExecutor<Guardian> {

    /**
     * @param userIds ids de cuentas
     * @return los perfiles de acudiente de esas cuentas, en una sola consulta
     */
    List<Guardian> findByUserIdIn(Collection<Integer> userIds);

    /**
     * Página de acudientes filtrados, con su cuenta cargada en la misma consulta.
     *
     * @param specification filtros
     * @param pageable      página y orden
     * @return la página de acudientes
     */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Guardian> findAll(Specification<Guardian> specification, Pageable pageable);

    /**
     * @param userId id de la cuenta
     * @return el perfil de acudiente de esa cuenta, si existe
     */
    Optional<Guardian> findByUserId(Integer userId);
}
