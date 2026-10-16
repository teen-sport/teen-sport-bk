package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.DisciplineRequest;
import com.uniquindio.teen_sport.dto.DisciplineResponse;
import com.uniquindio.teen_sport.dto.StatusRequest;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.DisciplineService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Catálogo de disciplinas (H1–H5). Consultar está permitido a cualquier rol; crear, editar y
 * cambiar el estado es exclusivo de {@code ADMIN}.
 */
@RestController
@RequestMapping("/disciplines")
public class DisciplineController {

    private final DisciplineService disciplineService;
    private final CurrentUserService currentUserService;

    /**
     * @param disciplineService  casos de uso del catálogo
     * @param currentUserService rol de quien consulta, tomado del JWT
     */
    public DisciplineController(DisciplineService disciplineService, CurrentUserService currentUserService) {
        this.disciplineService = disciplineService;
        this.currentUserService = currentUserService;
    }

    /**
     * H1: listado; quien no es administrador solo recibe disciplinas activas.
     *
     * @param status filtro por estado (solo para administradores)
     * @return 200 con las disciplinas
     */
    @GetMapping
    public List<DisciplineResponse> listDisciplines(@RequestParam(required = false) EntityStatus status) {
        return disciplineService.list(currentUserService.current().role(), status);
    }

    /**
     * H2: detalle de una disciplina.
     *
     * @param id id de la disciplina
     * @return 200 con la disciplina
     */
    @GetMapping("/{id}")
    public DisciplineResponse getDiscipline(@PathVariable Integer id) {
        return disciplineService.get(id);
    }

    /**
     * H3: crea una disciplina activa.
     *
     * @param request datos de la disciplina
     * @return 201 con la disciplina y la cabecera {@code Location}
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisciplineResponse> createDiscipline(@Valid @RequestBody DisciplineRequest request) {
        DisciplineResponse discipline = disciplineService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(WebConfig.API_PREFIX + "/disciplines/{id}")
                .buildAndExpand(discipline.id())
                .toUri();
        return ResponseEntity.created(location).body(discipline);
    }

    /**
     * H4: edita una disciplina.
     *
     * @param id      id de la disciplina
     * @param request nuevos datos
     * @return 200 con la disciplina actualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DisciplineResponse updateDiscipline(@PathVariable Integer id, @Valid @RequestBody DisciplineRequest request) {
        return disciplineService.update(id, request);
    }

    /**
     * H5: activa o desactiva una disciplina.
     *
     * @param id      id de la disciplina
     * @param request nuevo estado
     * @return 200 con la disciplina actualizada
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public DisciplineResponse updateDisciplineStatus(@PathVariable Integer id,
                                                     @Valid @RequestBody StatusRequest request) {
        return disciplineService.changeStatus(id, request.status());
    }
}
