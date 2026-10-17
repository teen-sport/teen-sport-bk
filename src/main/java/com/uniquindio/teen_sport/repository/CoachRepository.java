package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.EntityStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a los perfiles de entrenador.
 */
public interface CoachRepository extends JpaRepository<Coach, Integer> {

    /**
     * @param userIds ids de cuentas
     * @return los perfiles de entrenador de esas cuentas, en una sola consulta
     */
    List<Coach> findByUserIdIn(Collection<Integer> userIds);

    /**
     * @param id id del entrenador
     * @return el entrenador con su cuenta cargada, si existe
     */
    @EntityGraph(attributePaths = "user")
    Optional<Coach> findWithUserById(Integer id);

    /**
     * Bloquea al entrenador hasta el fin de la transacción. Se usa al asignarle franjas, para que dos
     * cambios simultáneos no le dejen horarios cruzados.
     *
     * @param id id del entrenador
     * @return el entrenador bloqueado
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coach c where c.id = :id")
    Optional<Coach> findLockedById(@Param("id") Integer id);

    /**
     * @param sort orden
     * @return todos los entrenadores con su cuenta cargada
     */
    @EntityGraph(attributePaths = "user")
    List<Coach> findAllBy(Sort sort);

    /**
     * @param status estado de la cuenta del entrenador
     * @param sort   orden
     * @return los entrenadores con ese estado, con su cuenta cargada
     */
    @EntityGraph(attributePaths = "user")
    List<Coach> findByUserStatus(EntityStatus status, Sort sort);

    /**
     * @param userId id de la cuenta
     * @return el perfil de entrenador de esa cuenta, si existe
     */
    Optional<Coach> findByUserId(Integer userId);
}
