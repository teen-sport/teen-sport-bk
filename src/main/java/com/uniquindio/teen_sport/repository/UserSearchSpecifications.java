package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros parametrizados de los listados administrativos de usuarios (U6) y acudientes (U12).
 *
 * <p>La búsqueda libre {@code q} compara, sin distinguir mayúsculas, con nombres, apellidos, nombre
 * completo, correo y número de documento. Los comodines {@code %} y {@code _} escritos por el
 * usuario se escapan para que se busquen como texto literal.</p>
 */
public final class UserSearchSpecifications {

    private static final char ESCAPE = '\\';

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
            if (hasText(text)) {
                predicates.add(matchesText(cb, root, text));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * @param text texto libre, o {@code null}/vacío para no filtrar
     * @return filtro de acudientes por los datos de su cuenta
     */
    public static Specification<Guardian> guardians(String text) {
        return (root, query, cb) -> hasText(text) ? matchesText(cb, root.get("user"), text) : cb.conjunction();
    }

    private static Predicate matchesText(CriteriaBuilder cb, Path<?> user, String text) {
        String pattern = "%" + escapeWildcards(text.trim().toLowerCase(Locale.ROOT)) + "%";
        Expression<String> firstName = cb.lower(user.<String>get("firstName"));
        Expression<String> lastName = cb.lower(user.<String>get("lastName"));
        Expression<String> fullName = cb.concat(cb.concat(firstName, " "), lastName);
        return cb.or(
                cb.like(firstName, pattern, ESCAPE),
                cb.like(lastName, pattern, ESCAPE),
                cb.like(fullName, pattern, ESCAPE),
                cb.like(cb.lower(user.<String>get("email")), pattern, ESCAPE),
                cb.like(cb.lower(user.<String>get("documentNumber")), pattern, ESCAPE));
    }

    private static String escapeWildcards(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
