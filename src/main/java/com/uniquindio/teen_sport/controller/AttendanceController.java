package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.AttendanceRecordResponse;
import com.uniquindio.teen_sport.dto.AttendanceRoster;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.RecordAttendanceRequest;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.repository.AttendanceFilter;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.AttendanceQueryService;
import com.uniquindio.teen_sport.service.AttendanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asistencia (A7–A9). Solo el entrenador del grupo registra la asistencia; el administrador también
 * consulta la lista. El historial está disponible para los tres roles, cada uno en su alcance.
 */
@RestController
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceQueryService queryService;
    private final CurrentUserService currentUserService;

    /**
     * @param attendanceService  lista y registro de asistencia
     * @param queryService       historial de asistencia
     * @param currentUserService identidad del usuario autenticado
     */
    public AttendanceController(AttendanceService attendanceService, AttendanceQueryService queryService,
                                CurrentUserService currentUserService) {
        this.attendanceService = attendanceService;
        this.queryService = queryService;
        this.currentUserService = currentUserService;
    }

    /**
     * A7: lista de asistencia con las marcas guardadas.
     *
     * @param id id de la sesión
     * @return 200 con la lista
     */
    @GetMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public AttendanceRoster getSessionAttendance(@PathVariable Integer id) {
        return attendanceService.roster(currentUserService.current(), id);
    }

    /**
     * A8: registra la asistencia de toda la lista.
     *
     * @param id      id de la sesión
     * @param request una marca por deportista
     * @return 200 con la lista guardada
     */
    @PutMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasRole('COACH')")
    public AttendanceRoster recordSessionAttendance(@PathVariable Integer id,
                                                    @Valid @RequestBody RecordAttendanceRequest request) {
        return attendanceService.record(currentUserService.current(), id, request);
    }

    /**
     * A9: historial paginado de asistencia.
     *
     * @param page      índice de página, desde 0
     * @param size      tamaño de página, de 1 a 100
     * @param athleteId filtro por deportista
     * @param groupId   filtro por grupo
     * @param from      fecha de sesión mínima, incluida
     * @param to        fecha de sesión máxima, incluida
     * @param status    filtro por marca
     * @return 200 con la página
     */
    @GetMapping("/attendance-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH', 'GUARDIAN')")
    public PageResponse<AttendanceRecordResponse> listAttendanceRecords(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AttendanceStatus status) {
        return queryService.list(currentUserService.current(),
                new AttendanceFilter(athleteId, groupId, from, to, status, null, null), page, size);
    }
}
