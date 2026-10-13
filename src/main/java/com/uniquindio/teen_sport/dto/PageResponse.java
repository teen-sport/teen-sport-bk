package com.uniquindio.teen_sport.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Página de resultados (esquema {@code PageMetadata} más {@code content}).
 *
 * @param content       elementos de la página
 * @param page          índice de la página, desde 0
 * @param size          tamaño de página solicitado
 * @param totalElements total de elementos
 * @param totalPages    total de páginas
 * @param <T>           tipo de los elementos
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * @param page    página de Spring Data
     * @param content elementos ya convertidos a DTO, en el mismo orden
     * @param <T>     tipo de los elementos
     * @return la página con los metadatos del contrato
     */
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
