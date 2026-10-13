package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.GroupAvailability;
import com.uniquindio.teen_sport.service.AvailabilityService;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disponibilidad de cupos (E5), para administradores y acudientes.
 */
@RestController
@RequestMapping("/availability")
@PreAuthorize("hasAnyRole('ADMIN', 'GUARDIAN')")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /**
     * @param availabilityService disponibilidad por periodo
     */
    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * E5: grupos activos con sus cupos en el periodo.
     *
     * @param period       mes {@code YYYY-MM}, desde el mes actual
     * @param disciplineId filtro por disciplina
     * @return 200 con la disponibilidad de cada grupo
     */
    @GetMapping
    public List<GroupAvailability> getAvailability(@RequestParam YearMonth period,
                                                   @RequestParam(required = false) Integer disciplineId) {
        return availabilityService.list(period, disciplineId);
    }
}
