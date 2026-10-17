package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Athlete;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados del listado de deportistas (E2).
 */
public final class AthleteSpecifications {

    private AthleteSpecifications() {
    }

    /**
     * @param guardianId acudiente cuyos deportistas se listan, o {@code null} para todos
     * @param text       texto libre sobre nombres, apellidos y documento ({@link TextSearch}), o {@code null}
     * @return filtro de deportistas
     */
    public static Specification<Athlete> athletes(Integer guardianId, String text) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (guardianId != null) {
                predicates.add(cb.equal(root.get("guardian").get("id"), guardianId));
            }
            if (TextSearch.hasText(text)) {
                predicates.add(TextSearch.matches(cb, root, text, "documentNumber"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
