package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.EntityStatus;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso al catálogo de disciplinas.
 */
public interface DisciplineRepository extends JpaRepository<Discipline, Integer> {

    /**
     * @param status estado buscado
     * @param sort   orden
     * @return las disciplinas con ese estado
     */
    List<Discipline> findByStatus(EntityStatus status, Sort sort);

    /**
     * @param name nombre, sin distinguir mayúsculas
     * @return {@code true} si alguna disciplina ya usa el nombre
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * @param name nombre, sin distinguir mayúsculas
     * @param id   disciplina que se excluye de la comprobación (la que se está editando)
     * @return {@code true} si otra disciplina ya usa el nombre
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
