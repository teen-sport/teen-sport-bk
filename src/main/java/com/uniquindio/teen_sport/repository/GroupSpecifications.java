package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados de los listados de grupos (H6 y E5).
 */
public final class GroupSpecifications {

    private GroupSpecifications() {
    }

    /**
     * @param disciplineId disciplina, o {@code null} para no filtrar
     * @param coachId      entrenador, o {@code null} para no filtrar
     * @param status       estado, o {@code null} para no filtrar
     * @return filtro de grupos
     */
    public static Specification<Group> groups(Integer disciplineId, Integer coachId, EntityStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (disciplineId != null) {
                predicates.add(cb.equal(root.get("discipline").get("id"), disciplineId));
            }
            if (coachId != null) {
                predicates.add(cb.equal(root.get("coach").get("id"), coachId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Grupos que admiten matrículas: activos y de una disciplina activa (E5).
     *
     * @param disciplineId disciplina, o {@code null} para no filtrar
     * @return filtro de grupos
     */
    public static Specification<Group> openForEnrollment(Integer disciplineId) {
        Specification<Group> open = (root, query, cb) -> cb.equal(root.get("discipline").get("status"),
                EntityStatus.ACTIVE);
        return open.and(groups(disciplineId, null, EntityStatus.ACTIVE));
    }
}
