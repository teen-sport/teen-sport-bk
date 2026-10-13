package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las matrículas.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer>, JpaSpecificationExecutor<Enrollment> {

    /**
     * Matrículas filtradas con deportista, grupo y disciplina cargados en la misma consulta.
     *
     * @param specification filtros
     * @param pageable      página y orden
     * @return la página de matrículas
     */
    @Override
    @EntityGraph(attributePaths = {"athlete", "group", "group.discipline"})
    Page<Enrollment> findAll(Specification<Enrollment> specification, Pageable pageable);

    /**
     * @param id id de la matrícula
     * @return la matrícula con deportista, grupo y disciplina cargados
     */
    @EntityGraph(attributePaths = {"athlete", "group", "group.discipline"})
    Optional<Enrollment> findDetailedById(Integer id);

    /**
     * Bloquea la matrícula hasta el fin de la transacción, para que dos cambios de estado o de grupo
     * simultáneos sobre ella se serialicen. No carga sus relaciones en la misma consulta: así solo se
     * bloquea la fila de la matrícula y no las del deportista o el grupo, que otros casos de uso
     * bloquean en su propio orden.
     *
     * @param id id de la matrícula
     * @return la matrícula bloqueada
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.id = :id")
    Optional<Enrollment> findLockedById(@Param("id") Integer id);

    /**
     * Cupos ocupados de varios grupos en una sola consulta. Los grupos sin matrículas en esos estados
     * no aparecen en el resultado.
     *
     * @param groupIds ids de grupos
     * @param period   periodo consultado, como texto {@code YYYY-MM}
     * @param statuses estados que ocupan cupo
     * @return la ocupación de cada grupo con matrículas
     */
    @Query("""
            select e.group.id as groupId,
                   sum(case when e.periodMonth <= :period then 1 else 0 end) as startedBy,
                   count(e) as inForce
            from Enrollment e
            where e.group.id in :groupIds and e.status in :statuses
            group by e.group.id
            """)
    List<GroupOccupancy> countOccupancy(@Param("groupIds") Collection<Integer> groupIds,
                                        @Param("period") String period,
                                        @Param("statuses") Collection<EnrollmentStatus> statuses);

    /**
     * @param groupId  id del grupo
     * @param statuses estados
     * @return matrículas del grupo en esos estados
     */
    long countByGroupIdAndStatusIn(Integer groupId, Collection<EnrollmentStatus> statuses);

    /**
     * @param groupId  id del grupo
     * @param statuses estados
     * @return {@code true} si el grupo tiene alguna matrícula en esos estados
     */
    boolean existsByGroupIdAndStatusIn(Integer groupId, Collection<EnrollmentStatus> statuses);

    /**
     * @param athleteId    id del deportista
     * @param disciplineId id de la disciplina
     * @param statuses     estados
     * @return {@code true} si el deportista tiene una matrícula en esos estados en algún grupo de la disciplina
     */
    boolean existsByAthleteIdAndGroupDisciplineIdAndStatusIn(Integer athleteId, Integer disciplineId,
                                                             Collection<EnrollmentStatus> statuses);

    /**
     * @param athleteId id del deportista
     * @param statuses  estados
     * @return {@code true} si el deportista tiene alguna matrícula en esos estados
     */
    boolean existsByAthleteIdAndStatusIn(Integer athleteId, Collection<EnrollmentStatus> statuses);

    /**
     * @param athleteIds ids de deportistas
     * @param statuses   estados
     * @return los deportistas, de entre los indicados, que tienen alguna matrícula en esos estados
     */
    @Query("select distinct e.athlete.id from Enrollment e where e.athlete.id in :athleteIds and e.status in :statuses")
    List<Integer> findAthleteIdsWithStatus(@Param("athleteIds") Collection<Integer> athleteIds,
                                           @Param("statuses") Collection<EnrollmentStatus> statuses);
}
