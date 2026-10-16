package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.CreateSessionRequest;
import com.uniquindio.teen_sport.dto.GenerateSessionsRequest;
import com.uniquindio.teen_sport.dto.GenerateSessionsResponse;
import com.uniquindio.teen_sport.dto.ReasonRequiredRequest;
import com.uniquindio.teen_sport.dto.SessionResponse;
import com.uniquindio.teen_sport.dto.UpdateSessionRequest;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.repository.SessionFilter;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.SessionQueryService;
import com.uniquindio.teen_sport.service.SessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Sesiones de entrenamiento (A1–A6). Consultar está permitido a los tres roles, cada uno en su alcance;
 * programar, reprogramar y cancelar, al administrador y al entrenador del grupo.
 */
@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final SessionQueryService queryService;
    private final CurrentUserService currentUserService;

    /**
     * @param sessionService     programación de sesiones
     * @param queryService       calendario y detalle
     * @param currentUserService identidad del usuario autenticado
     */
    public SessionController(SessionService sessionService, SessionQueryService queryService,
                             CurrentUserService currentUserService) {
        this.sessionService = sessionService;
        this.queryService = queryService;
        this.currentUserService = currentUserService;
    }

    /**
     * A1: crea una sesión individual.
     *
     * @param request datos de la sesión
     * @return 201 con la sesión y la cabecera {@code Location}
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionResponse session = sessionService.create(currentUserService.current(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(WebConfig.API_PREFIX + "/sessions/{id}")
                .buildAndExpand(session.id())
                .toUri();
        return ResponseEntity.created(location).body(session);
    }

    /**
     * A3: calendario de sesiones en el alcance del rol.
     *
     * @param from         primera fecha; por defecto, el lunes de la semana actual
     * @param to           última fecha; por defecto, seis días después de {@code from}
     * @param groupId      filtro por grupo
     * @param disciplineId filtro por disciplina
     * @param athleteId    filtro por deportista
     * @param status       filtro por estado
     * @return 200 con las sesiones ordenadas por fecha y hora
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH', 'GUARDIAN')")
    public List<SessionResponse> listSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer disciplineId,
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) SessionStatus status) {
        return queryService.list(currentUserService.current(),
                new SessionFilter(from, to, groupId, disciplineId, status, null, null), athleteId);
    }

    /**
     * A2: genera sesiones a partir de las franjas semanales del grupo.
     *
     * @param request grupo y rango
     * @return 201 con el resultado
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateSessionsResponse generateSessions(@Valid @RequestBody GenerateSessionsRequest request) {
        return sessionService.generate(currentUserService.current(), request);
    }

    /**
     * A4: detalle de una sesión.
     *
     * @param id id de la sesión
     * @return 200 con la sesión
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH', 'GUARDIAN')")
    public SessionResponse getSession(@PathVariable Integer id) {
        return queryService.get(currentUserService.current(), id);
    }

    /**
     * A5: reprograma una sesión.
     *
     * @param id      id de la sesión
     * @param request nueva fecha, horas y notas
     * @return 200 con la sesión actualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public SessionResponse updateSession(@PathVariable Integer id, @Valid @RequestBody UpdateSessionRequest request) {
        return sessionService.reschedule(currentUserService.current(), id, request);
    }

    /**
     * A6: cancela una sesión.
     *
     * @param id      id de la sesión
     * @param request motivo
     * @return 200 con la sesión cancelada
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public SessionResponse cancelSession(@PathVariable Integer id, @Valid @RequestBody ReasonRequiredRequest request) {
        return sessionService.cancel(currentUserService.current(), id, request.reason());
    }
}
