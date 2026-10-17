package com.uniquindio.teen_sport.dto;

import java.util.List;

/**
 * Resultado de generar sesiones (esquema {@code GenerateSessionsResponse}).
 *
 * @param created  sesiones creadas
 * @param skipped  sesiones omitidas porque ya existían
 * @param sessions sesiones creadas, ordenadas por fecha y hora
 */
public record GenerateSessionsResponse(int created, int skipped, List<SessionResponse> sessions) {
}
