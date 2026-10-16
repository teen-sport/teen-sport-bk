package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.TrainingSession;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados del calendario de sesiones (A3).
 */
public final class SessionSpecifications {

    private SessionSpecifications() {
    }

    /**
     * @param filter filtros; el rango de fechas es obligatorio
     * @return filtro de sesiones
     */
    public static Specification<TrainingSession> sessions(SessionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Path<Object> group = root.get("group");
            predicates.add(cb.between(root.get("sessionDate"), filter.from(), filter.to()));
            if (filter.groupId() != null) {
                predicates.add(cb.equal(group.get("id"), filter.groupId()));
            }
            if (filter.disciplineId() != null) {
                predicates.add(cb.equal(group.get("discipline").get("id"), filter.disciplineId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.coachId() != null) {
                predicates.add(cb.equal(group.get("coach").get("id"), filter.coachId()));
            }
            if (filter.groupIds() != null) {
                predicates.add(group.get("id").in(filter.groupIds()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
