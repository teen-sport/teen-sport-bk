package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Athlete;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a los deportistas.
 */
public interface AthleteRepository extends JpaRepository<Athlete, Integer>, JpaSpecificationExecutor<Athlete> {

    /**
     * @param documentNumber número de documento
     * @return {@code true} si algún deportista ya usa el documento
     */
    boolean existsByDocumentNumber(String documentNumber);

    /**
     * @param documentNumber número de documento
     * @param id             deportista que se excluye de la comprobación (el que se está editando)
     * @return {@code true} si otro deportista ya usa el documento
     */
    boolean existsByDocumentNumberAndIdNot(String documentNumber, Integer id);

    /**
     * Bloquea el deportista hasta el fin de la transacción, para serializar sus solicitudes de
     * matrícula y evitar dos matrículas vigentes en la misma disciplina.
     *
     * @param id id del deportista
     * @return el deportista bloqueado
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Athlete a where a.id = :id")
    Optional<Athlete> findLockedById(@Param("id") Integer id);
}
