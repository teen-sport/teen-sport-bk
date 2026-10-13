package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.AthleteResponse;
import com.uniquindio.teen_sport.dto.CreateAthleteRequest;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.UpdateAthleteRequest;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.AthleteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Deportistas (E1–E4), para administradores y acudientes. La propiedad de cada deportista se
 * comprueba en el servicio con la identidad del token.
 */
@RestController
@RequestMapping("/athletes")
@PreAuthorize("hasAnyRole('ADMIN', 'GUARDIAN')")
public class AthleteController {

    private final AthleteService athleteService;
    private final CurrentUserService currentUserService;

    /**
     * @param athleteService     casos de uso de deportistas
     * @param currentUserService identidad del usuario autenticado
     */
    public AthleteController(AthleteService athleteService, CurrentUserService currentUserService) {
        this.athleteService = athleteService;
        this.currentUserService = currentUserService;
    }

    /**
     * E1: registra un deportista.
     *
     * @param request datos del deportista
     * @return 201 con el deportista y la cabecera {@code Location}
     */
    @PostMapping
    public ResponseEntity<AthleteResponse> createAthlete(@Valid @RequestBody CreateAthleteRequest request) {
        AthleteResponse athlete = athleteService.create(currentUserService.current(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(WebConfig.API_PREFIX + "/athletes/{id}")
                .buildAndExpand(athlete.id())
                .toUri();
        return ResponseEntity.created(location).body(athlete);
    }

    /**
     * E2: listado paginado; un acudiente solo recibe los suyos.
     *
     * @param page       índice de página, desde 0
     * @param size       tamaño de página, de 1 a 100
     * @param q          búsqueda libre sobre nombres y documento
     * @param guardianId filtro por acudiente (solo administrador)
     * @return 200 con la página
     */
    @GetMapping
    public PageResponse<AthleteResponse> listAthletes(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                      @RequestParam(required = false) @Size(max = 150) String q,
                                                      @RequestParam(required = false) Integer guardianId) {
        return athleteService.list(currentUserService.current(), guardianId, q, page, size);
    }

    /**
     * E3: detalle de un deportista.
     *
     * @param id id del deportista
     * @return 200 con el deportista
     */
    @GetMapping("/{id}")
    public AthleteResponse getAthlete(@PathVariable Integer id) {
        return athleteService.get(currentUserService.current(), id);
    }

    /**
     * E4: edita un deportista.
     *
     * @param id      id del deportista
     * @param request nuevos datos
     * @return 200 con el deportista actualizado
     */
    @PutMapping("/{id}")
    public AthleteResponse updateAthlete(@PathVariable Integer id, @Valid @RequestBody UpdateAthleteRequest request) {
        return athleteService.update(currentUserService.current(), id, request);
    }
}
