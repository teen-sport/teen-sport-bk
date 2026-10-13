package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a los grupos de entrenamiento.
 */
public interface GroupRepository extends JpaRepository<Group, Integer>, JpaSpecificationExecutor<Group> {

    /**
     * Grupos filtrados con disciplina, entrenador y cuenta del entrenador cargados en la misma consulta.
     *
     * @param specification filtros
     * @param sort          orden
     * @return los grupos
     */
    @Override
    @EntityGraph(attributePaths = {"discipline", "coach", "coach.user"})
    List<Group> findAll(Specification<Group> specification, Sort sort);

    /**
     * @param id id del grupo
     * @return el grupo con disciplina, entrenador y cuenta del entrenador cargados
     */
    @EntityGraph(attributePaths = {"discipline", "coach", "coach.user"})
    Optional<Group> findDetailedById(Integer id);

    /**
     * Bloquea el grupo hasta el fin de la transacción, para serializar los cambios de sus horarios y
     * de su entrenador.
     *
     * @param id id del grupo
     * @return el grupo bloqueado
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Group g where g.id = :id")
    Optional<Group> findLockedById(@Param("id") Integer id);

    /**
     * @param coachId id del entrenador
     * @param status  estado de los grupos
     * @return {@code true} si el entrenador tiene algún grupo con ese estado
     */
    boolean existsByCoachIdAndStatus(Integer coachId, EntityStatus status);

    /**
     * @param disciplineId id de la disciplina
     * @param status       estado de los grupos
     * @return {@code true} si la disciplina tiene algún grupo con ese estado
     */
    boolean existsByDisciplineIdAndStatus(Integer disciplineId, EntityStatus status);
}
