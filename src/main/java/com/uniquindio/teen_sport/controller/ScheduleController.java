package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.ScheduleRequest;
import com.uniquindio.teen_sport.dto.ScheduleResponse;
import com.uniquindio.teen_sport.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Franjas semanales ya creadas (H13, H14). Exclusivo de {@code ADMIN}; la creación (H12) está en
 * {@link GroupController} porque cuelga del grupo.
 */
@RestController
@RequestMapping("/schedules")
@PreAuthorize("hasRole('ADMIN')")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * @param scheduleService casos de uso de franjas
     */
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * H13: edita una franja.
     *
     * @param id      id de la franja
     * @param request día y horas
     * @return 200 con la franja actualizada
     */
    @PutMapping("/{id}")
    public ScheduleResponse updateSchedule(@PathVariable Integer id, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.update(id, request);
    }

    /**
     * H14: elimina una franja.
     *
     * @param id id de la franja
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable Integer id) {
        scheduleService.delete(id);
    }
}
