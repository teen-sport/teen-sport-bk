package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las sesiones de entrenamiento.
 */
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Integer>,
        JpaSpecificationExecutor<TrainingSession> {

    /**
     * Sesiones filtradas con grupo, disciplina, entrenador y cuenta del entrenador cargados en la misma
     * consulta.
     *
     * @param specification filtros
     * @param sort          orden
     * @return las sesiones
     */
    @Override
    @EntityGraph(attributePaths = {"group", "group.discipline", "coach", "coach.user"})
    List<TrainingSession> findAll(Specification<TrainingSession> specification, Sort sort);

    /**
     * @param id id de la sesión
     * @return la sesión con grupo, disciplina, entrenador y cuenta del entrenador cargados
     */
    @EntityGraph(attributePaths = {"group", "group.discipline", "coach", "coach.user"})
    Optional<TrainingSession> findDetailedById(Integer id);

    /**
     * Bloquea la sesión hasta el fin de la transacción, para serializar su reprogramación, su
     * cancelación y el registro de su asistencia.
     *
     * @param id id de la sesión
     * @return la sesión bloqueada
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TrainingSession s where s.id = :id")
    Optional<TrainingSession> findLockedById(@Param("id") Integer id);

    /**
     * Grupo de una sesión sin cargarla, para bloquear primero el grupo y después la sesión.
     *
     * @param id id de la sesión
     * @return el id del grupo de la sesión
     */
    @Query("select s.group.id from TrainingSession s where s.id = :id")
    Optional<Integer> findGroupIdById(@Param("id") Integer id);

    /**
     * @param groupId   id del grupo
     * @param date      fecha
     * @param startTime hora de inicio
     * @return {@code true} si el grupo ya tiene una sesión con esa fecha y hora de inicio
     */
    boolean existsByGroupIdAndSessionDateAndStartTime(Integer groupId, LocalDate date, LocalTime startTime);

    /**
     * @param groupId   id del grupo
     * @param date      fecha
     * @param startTime hora de inicio
     * @param id        sesión que se excluye
     * @return {@code true} si otra sesión del grupo tiene esa fecha y hora de inicio
     */
    boolean existsByGroupIdAndSessionDateAndStartTimeAndIdNot(Integer groupId, LocalDate date, LocalTime startTime,
                                                              Integer id);

    /**
     * @param groupId id del grupo
     * @param from    primera fecha, incluida
     * @param to      última fecha, incluida
     * @return las sesiones del grupo en ese rango
     */
    List<TrainingSession> findByGroupIdAndSessionDateBetween(Integer groupId, LocalDate from, LocalDate to);

    /**
     * @param groupId id del grupo
     * @param status  estado
     * @param date    primera fecha, incluida
     * @return {@code true} si el grupo tiene sesiones en ese estado desde esa fecha
     */
    boolean existsByGroupIdAndStatusAndSessionDateGreaterThanEqual(Integer groupId, SessionStatus status,
                                                                   LocalDate date);

    /**
     * Pasa al entrenador indicado las sesiones programadas del grupo desde una fecha. Las anteriores
     * conservan el entrenador que las dictó.
     *
     * @param groupId id del grupo
     * @param coach   nuevo entrenador
     * @param from    primera fecha, incluida
     * @return sesiones actualizadas
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update TrainingSession s set s.coach = :coach
            where s.group.id = :groupId
              and s.status = com.uniquindio.teen_sport.entity.SessionStatus.SCHEDULED
              and s.sessionDate >= :from
            """)
    int reassignScheduledFrom(@Param("groupId") Integer groupId, @Param("coach") Coach coach,
                              @Param("from") LocalDate from);
}
