package com.uniquindio.teen_sport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Rango para generar sesiones a partir de las franjas de un grupo (esquema {@code GenerateSessionsRequest}).
 *
 * <p>El campo del contrato es {@code notify}; en Java se llama {@code notifyGuardians} porque un
 * componente de record no puede llamarse como {@link Object#notify()}.</p>
 *
 * @param groupId         id del grupo
 * @param dateFrom        primera fecha, incluida
 * @param dateTo          última fecha, incluida
 * @param notifyGuardians si se avisa a los acudientes; el correo llega con la Fase 7
 */
public record GenerateSessionsRequest(
        @NotNull Integer groupId,
        @NotNull LocalDate dateFrom,
        @NotNull LocalDate dateTo,
        @JsonProperty("notify") Boolean notifyGuardians) {
}
