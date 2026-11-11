package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.AttendanceRecord;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las marcas de asistencia.
 */
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer>,
        JpaSpecificationExecutor<AttendanceRecord> {

    /**
     * Marcas filtradas con la sesión, el grupo, la disciplina y el deportista cargados en la misma
     * consulta.
     *
     * @param specification filtros
     * @param pageable      página y orden
     * @return la página de marcas
     */
    @Override
    @EntityGraph(attributePaths = {"session", "session.group", "session.group.discipline", "athlete"})
    Page<AttendanceRecord> findAll(Specification<AttendanceRecord> specification, Pageable pageable);

    /**
     * @param sessionId id de la sesión
     * @return las marcas guardadas de la sesión
     */
    List<AttendanceRecord> findBySessionId(Integer sessionId);

    /**
     * R2: marcas por grupo en un rango de fechas. Los filtros nulos no limitan la consulta.
     *
     * @param from         primera fecha de sesión, incluida
     * @param to           última fecha de sesión, incluida
     * @param groupId      grupo, o {@code null}
     * @param disciplineId disciplina, o {@code null}
     * @param coachId      entrenador actual del grupo, o {@code null}
     * @return un conteo por grupo con marcas
     */
    @Query("""
            select g.id as groupId, g.name as groupName,
                   sum(case when a.status = com.uniquindio.teen_sport.entity.AttendanceStatus.PRESENT then 1 else 0 end) as present,
                   sum(case when a.status = com.uniquindio.teen_sport.entity.AttendanceStatus.ABSENT then 1 else 0 end) as absent
            from AttendanceRecord a join a.session s join s.group g join g.discipline d
            where s.sessionDate between :from and :to
              and (:groupId is null or g.id = :groupId)
              and (:disciplineId is null or d.id = :disciplineId)
              and (:coachId is null or g.coach.id = :coachId)
            group by g.id, g.name
            """)
    List<GroupMarkCount> countMarksByGroup(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                           @Param("groupId") Integer groupId,
                                           @Param("disciplineId") Integer disciplineId,
                                           @Param("coachId") Integer coachId);

    /**
     * R3: marcas de un deportista por grupo. Los filtros nulos no limitan la consulta.
     *
     * @param athleteId id del deportista
     * @param from      primera fecha de sesión, incluida, o {@code null}
     * @param to        última fecha de sesión, incluida, o {@code null}
     * @param coachId   entrenador actual del grupo, o {@code null}
     * @return un conteo por grupo con marcas, ordenado por nombre del grupo
     */
    @Query("""
            select g.id as groupId, g.name as groupName,
                   sum(case when a.status = com.uniquindio.teen_sport.entity.AttendanceStatus.PRESENT then 1 else 0 end) as present,
                   sum(case when a.status = com.uniquindio.teen_sport.entity.AttendanceStatus.ABSENT then 1 else 0 end) as absent
            from AttendanceRecord a join a.session s join s.group g
            where a.athlete.id = :athleteId
              and (cast(:from as LocalDate) is null or s.sessionDate >= :from)
              and (cast(:to as LocalDate) is null or s.sessionDate <= :to)
              and (:coachId is null or g.coach.id = :coachId)
            group by g.id, g.name
            order by g.name, g.id
            """)
    List<GroupMarkCount> countAthleteMarksByGroup(@Param("athleteId") Integer athleteId,
                                                  @Param("from") LocalDate from, @Param("to") LocalDate to,
                                                  @Param("coachId") Integer coachId);

    /**
     * R4: marcas de los deportistas con matrícula en esos estados en el grupo de la marca, de la más
     * reciente a la más antigua dentro de cada deportista y grupo. Los filtros nulos no limitan la
     * consulta.
     *
     * @param groupId  grupo, o {@code null}
     * @param coachId  entrenador actual del grupo, o {@code null}
     * @param from     primera fecha de sesión, incluida, o {@code null}
     * @param statuses estados de matrícula que mantienen al deportista en el grupo
     * @return las marcas, agrupadas por deportista y grupo
     */
    @Query("""
            select ath.id as athleteId, ath.firstName as firstNames, ath.lastName as lastNames,
                   g.id as groupId, g.name as groupName, a.status as status, s.sessionDate as sessionDate
            from AttendanceRecord a join a.athlete ath join a.session s join s.group g
            where (:groupId is null or g.id = :groupId)
              and (:coachId is null or g.coach.id = :coachId)
              and (cast(:from as LocalDate) is null or s.sessionDate >= :from)
              and exists (select e.id from Enrollment e
                          where e.athlete = ath and e.group = g and e.status in :statuses)
            order by ath.id, g.id, s.sessionDate desc, s.startTime desc, a.id desc
            """)
    List<AthleteGroupMark> findMarksOfEnrolledAthletes(@Param("groupId") Integer groupId,
                                                       @Param("coachId") Integer coachId,
                                                       @Param("from") LocalDate from,
                                                       @Param("statuses") Collection<EnrollmentStatus> statuses);
}
