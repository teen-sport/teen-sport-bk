package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.AttendanceRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
