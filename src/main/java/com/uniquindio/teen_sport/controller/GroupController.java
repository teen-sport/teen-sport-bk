package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.AssignCoachRequest;
import com.uniquindio.teen_sport.dto.CreateGroupRequest;
import com.uniquindio.teen_sport.dto.GroupResponse;
import com.uniquindio.teen_sport.dto.ScheduleRequest;
import com.uniquindio.teen_sport.dto.ScheduleResponse;
import com.uniquindio.teen_sport.dto.StatusRequest;
import com.uniquindio.teen_sport.dto.UpdateGroupRequest;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.GroupService;
import com.uniquindio.teen_sport.service.ScheduleService;
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
 * Grupos de entrenamiento (H6–H12). Consultar está permitido al administrador y al entrenador (solo
 * sus grupos); el resto de operaciones es exclusivo de {@code ADMIN}.
 */
@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final ScheduleService scheduleService;
    private final CurrentUserService currentUserService;

    /**
     * @param groupService       casos de uso de grupos
     * @param scheduleService    casos de uso de franjas
     * @param currentUserService identidad del usuario autenticado
     */
    public GroupController(GroupService groupService, ScheduleService scheduleService,
                           CurrentUserService currentUserService) {
        this.groupService = groupService;
        this.scheduleService = scheduleService;
        this.currentUserService = currentUserService;
    }

    /**
     * H6: grupos con sus franjas; un entrenador solo recibe los suyos.
     *
     * @param disciplineId filtro por disciplina
     * @param coachId      filtro por entrenador (solo administrador)
     * @param status       filtro por estado
     * @return 200 con los grupos
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public List<GroupResponse> listGroups(@RequestParam(required = false) Integer disciplineId,
                                          @RequestParam(required = false) Integer coachId,
                                          @RequestParam(required = false) EntityStatus status) {
        return groupService.list(currentUserService.current(), disciplineId, coachId, status);
    }

    /**
     * H7: detalle de un grupo.
     *
     * @param id id del grupo
     * @return 200 con el grupo
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public GroupResponse getGroup(@PathVariable Integer id) {
        return groupService.get(currentUserService.current(), id);
    }

    /**
     * H8: crea un grupo activo y sin franjas.
     *
     * @param request datos del grupo
     * @return 201 con el grupo y la cabecera {@code Location}
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        GroupResponse group = groupService.create(request);
        return ResponseEntity.created(location("/groups/{id}", group.id())).body(group);
    }

    /**
     * H9: edita el nombre y la capacidad.
     *
     * @param id      id del grupo
     * @param request nuevos datos
     * @return 200 con el grupo actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GroupResponse updateGroup(@PathVariable Integer id, @Valid @RequestBody UpdateGroupRequest request) {
        return groupService.update(id, request);
    }

    /**
     * H10: activa o desactiva un grupo.
     *
     * @param id      id del grupo
     * @param request nuevo estado
     * @return 200 con el grupo actualizado
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public GroupResponse updateGroupStatus(@PathVariable Integer id, @Valid @RequestBody StatusRequest request) {
        return groupService.changeStatus(id, request.status());
    }

    /**
     * H11: asigna o cambia el entrenador.
     *
     * @param id      id del grupo
     * @param request entrenador
     * @return 200 con el grupo actualizado
     */
    @PutMapping("/{id}/coach")
    @PreAuthorize("hasRole('ADMIN')")
    public GroupResponse assignGroupCoach(@PathVariable Integer id, @Valid @RequestBody AssignCoachRequest request) {
        return groupService.assignCoach(id, request.coachId());
    }

    /**
     * H12: agrega una franja semanal al grupo.
     *
     * @param id      id del grupo
     * @param request día y horas
     * @return 201 con la franja y la cabecera {@code Location}
     */
    @PostMapping("/{id}/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> createGroupSchedule(@PathVariable Integer id,
                                                                @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse schedule = scheduleService.create(id, request);
        return ResponseEntity.created(location("/schedules/{id}", schedule.id())).body(schedule);
    }

    private static URI location(String path, Integer id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(WebConfig.API_PREFIX + path)
                .buildAndExpand(id)
                .toUri();
    }
}
