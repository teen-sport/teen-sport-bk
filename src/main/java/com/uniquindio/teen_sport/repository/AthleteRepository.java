package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
