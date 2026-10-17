package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Schedule;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las franjas semanales de los grupos.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    /**
     * @param groupIds ids de grupos
     * @return las franjas de esos grupos en una sola consulta, ordenadas por día y hora
     */
    @Query("select s from Schedule s where s.group.id in :groupIds order by s.dayOfWeek, s.startTime")
    List<Schedule> findByGroupIds(@Param("groupIds") Collection<Integer> groupIds);

    /**
     * @param groupId id del grupo
     * @return las franjas del grupo
     */
    List<Schedule> findByGroupId(Integer groupId);

    /**
     * Franjas de los grupos activos de un entrenador, excepto las del grupo indicado. Son las que no
     * pueden cruzarse con las del grupo cuando ese entrenador lo tiene a cargo.
     *
     * @param coachId        id del entrenador
     * @param excludedGroupId grupo cuyas franjas se excluyen
     * @return las franjas
     */
    @Query("""
            select s from Schedule s
            where s.group.coach.id = :coachId
              and s.group.status = com.uniquindio.teen_sport.entity.EntityStatus.ACTIVE
              and s.group.id <> :excludedGroupId
            """)
    List<Schedule> findActiveByCoachExcludingGroup(@Param("coachId") Integer coachId,
                                                   @Param("excludedGroupId") Integer excludedGroupId);

    /**
     * @param id id de la franja
     * @return la franja con su grupo cargado
     */
    @EntityGraph(attributePaths = "group")
    Optional<Schedule> findWithGroupById(Integer id);
}
