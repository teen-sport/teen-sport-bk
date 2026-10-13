package com.uniquindio.teen_sport.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Búsqueda libre ({@code q}) común a los listados: compara sin distinguir mayúsculas y trata los
 * comodines {@code %} y {@code _} escritos por el usuario como texto literal.
 */
final class TextSearch {

    private static final char ESCAPE = '\\';

    private TextSearch() {
    }

    /**
     * @param text texto libre
     * @return {@code true} si hay algo que buscar
     */
    static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    /**
     * Coincidencia parcial del texto en el nombre, el apellido, el nombre completo o cualquiera de
     * los atributos de texto adicionales de la entidad.
     *
     * @param cb               constructor de criterios
     * @param person           entidad con {@code firstName} y {@code lastName}
     * @param text             texto libre
     * @param extraAttributes  otros atributos de texto en los que buscar (por ejemplo, documento)
     * @return predicado de coincidencia
     */
    static Predicate matches(CriteriaBuilder cb, Path<?> person, String text, String... extraAttributes) {
        String pattern = "%" + escapeWildcards(text.trim().toLowerCase(Locale.ROOT)) + "%";
        Expression<String> firstName = cb.lower(person.<String>get("firstName"));
        Expression<String> lastName = cb.lower(person.<String>get("lastName"));
        List<Predicate> options = new ArrayList<>(List.of(
                cb.like(firstName, pattern, ESCAPE),
                cb.like(lastName, pattern, ESCAPE),
                cb.like(cb.concat(cb.concat(firstName, " "), lastName), pattern, ESCAPE)));
        for (String attribute : extraAttributes) {
            options.add(cb.like(cb.lower(person.<String>get(attribute)), pattern, ESCAPE));
        }
        return cb.or(options.toArray(Predicate[]::new));
    }

    private static String escapeWildcards(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
