package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.PaymentHistory;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados del historial de pagos (E19).
 */
public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    /**
     * @param filter filtros
     * @return filtro de pagos
     */
    public static Specification<PaymentHistory> payments(PaymentFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Path<Object> enrollment = root.get("enrollment");
            if (filter.enrollmentId() != null) {
                predicates.add(cb.equal(enrollment.get("id"), filter.enrollmentId()));
            }
            if (filter.athleteId() != null) {
                predicates.add(cb.equal(enrollment.get("athlete").get("id"), filter.athleteId()));
            }
            if (filter.guardianId() != null) {
                predicates.add(cb.equal(enrollment.get("athlete").get("guardian").get("id"), filter.guardianId()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), filter.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
