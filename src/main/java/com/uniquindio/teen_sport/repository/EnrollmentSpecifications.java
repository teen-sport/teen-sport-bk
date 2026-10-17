package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados del listado de matrículas (E7).
 *
 * <p>{@code period} devuelve las matrículas vigentes en ese mes: las que empezaron en ese mes o antes
 * y, si no se filtra por estado, están {@code PENDING_REVIEW} o {@code ACTIVE}. Con un filtro de
 * estado explícito se usa ese estado (por ejemplo, las canceladas que habían empezado en ese mes o
 * antes).</p>
 */
public final class EnrollmentSpecifications {

    private EnrollmentSpecifications() {
    }

    /**
     * @param filter filtros
     * @return filtro de matrículas
     */
    public static Specification<Enrollment> enrollments(EnrollmentFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addEqual(predicates, cb, root.get("status"), filter.status());
            addEqual(predicates, cb, root.get("paymentStatus"), filter.paymentStatus());
            addEqual(predicates, cb, root.get("paymentReported"), filter.paymentReported());
            addEqual(predicates, cb, root.get("group").get("discipline").get("id"), filter.disciplineId());
            addEqual(predicates, cb, root.get("group").get("id"), filter.groupId());
            addEqual(predicates, cb, root.get("athlete").get("id"), filter.athleteId());
            addEqual(predicates, cb, root.get("athlete").get("guardian").get("id"), filter.guardianId());
            if (filter.period() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("periodMonth"), filter.period().toString()));
                if (filter.status() == null) {
                    predicates.add(root.get("status").in(EnrollmentStatus.IN_FORCE));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addEqual(List<Predicate> predicates, CriteriaBuilder cb, Path<?> path, Object value) {
        if (value != null) {
            predicates.add(cb.equal(path, value));
        }
    }
}
