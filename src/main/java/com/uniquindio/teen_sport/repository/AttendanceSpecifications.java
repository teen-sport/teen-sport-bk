package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.AttendanceRecord;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados del historial de asistencia (A9).
 */
public final class AttendanceSpecifications {

    private AttendanceSpecifications() {
    }

    /**
     * @param filter filtros
     * @return filtro de marcas de asistencia
     */
    public static Specification<AttendanceRecord> records(AttendanceFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Path<Object> session = root.get("session");
            Path<Object> athlete = root.get("athlete");
            if (filter.athleteId() != null) {
                predicates.add(cb.equal(athlete.get("id"), filter.athleteId()));
            }
            if (filter.groupId() != null) {
                predicates.add(cb.equal(session.get("group").get("id"), filter.groupId()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(session.get("sessionDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(session.get("sessionDate"), filter.to()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.coachId() != null) {
                predicates.add(cb.equal(session.get("group").get("coach").get("id"), filter.coachId()));
            }
            if (filter.guardianId() != null) {
                predicates.add(cb.equal(athlete.get("guardian").get("id"), filter.guardianId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
