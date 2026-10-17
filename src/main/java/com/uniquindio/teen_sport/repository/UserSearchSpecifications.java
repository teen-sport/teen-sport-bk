package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados de los listados administrativos de usuarios (U6) y acudientes (U12).
 *
 * <p>La búsqueda libre {@code q} ({@link TextSearch}) compara con nombres, apellidos, nombre
 * completo, correo y número de documento.</p>
 */
public final class UserSearchSpecifications {

    private UserSearchSpecifications() {
    }

    /**
     * @param role   rol exacto, o {@code null} para no filtrar
     * @param status estado exacto, o {@code null} para no filtrar
     * @param text   texto libre, o {@code null}/vacío para no filtrar
     * @return filtro de cuentas
     */
    public static Specification<User> users(RoleName role, EntityStatus status, String text) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role").get("name"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (TextSearch.hasText(text)) {
                predicates.add(TextSearch.matches(cb, root, text, "email", "documentNumber"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * @param text texto libre, o {@code null}/vacío para no filtrar
     * @return filtro de acudientes por los datos de su cuenta
     */
    public static Specification<Guardian> guardians(String text) {
        return (root, query, cb) -> TextSearch.hasText(text)
                ? TextSearch.matches(cb, root.get("user"), text, "email", "documentNumber")
                : cb.conjunction();
    }
}
