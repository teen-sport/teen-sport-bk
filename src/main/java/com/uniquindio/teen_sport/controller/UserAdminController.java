package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.CreateUserRequest;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.StatusRequest;
import com.uniquindio.teen_sport.dto.UpdateUserRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.UserAdministrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
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
 * Administración de cuentas (U6–U10). Todas las operaciones son exclusivas del rol {@code ADMIN}.
 *
 * <p>Comparte la ruta {@code /users} con {@link UserController}, que atiende el registro público y
 * la cuenta propia.</p>
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdministrationService administrationService;
    private final CurrentUserService currentUserService;

    /**
     * @param administrationService casos de uso de administración de cuentas
     * @param currentUserService    identidad del administrador autenticado
     */
    public UserAdminController(UserAdministrationService administrationService,
                               CurrentUserService currentUserService) {
        this.administrationService = administrationService;
        this.currentUserService = currentUserService;
    }

    /**
     * U6: listado paginado de cuentas.
     *
     * @param page   índice de página, desde 0
     * @param size   tamaño de página, de 1 a 100
     * @param role   filtro por rol
     * @param status filtro por estado
     * @param q      búsqueda libre sobre nombre, correo y documento
     * @return 200 con la página
     */
    @GetMapping
    public PageResponse<UserProfile> listUsers(@RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                               @RequestParam(required = false) RoleName role,
                                               @RequestParam(required = false) EntityStatus status,
                                               @RequestParam(required = false) @Size(max = 150) String q) {
        return administrationService.listUsers(role, status, q, page, size);
    }

    /**
     * U7: detalle de una cuenta.
     *
     * @param id id de la cuenta
     * @return 200 con el perfil
     */
    @GetMapping("/{id}")
    public UserProfile getUser(@PathVariable Integer id) {
        return administrationService.getUser(id);
    }

    /**
     * U8: crea una cuenta de administrador o de entrenador.
     *
     * @param request datos de la cuenta
     * @return 201 con el perfil creado y la cabecera {@code Location}
     */
    @PostMapping
    public ResponseEntity<UserProfile> createStaffUser(@Valid @RequestBody CreateUserRequest request) {
        UserProfile profile = administrationService.createStaffUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(WebConfig.API_PREFIX + "/users/{id}")
                .buildAndExpand(profile.id())
                .toUri();
        return ResponseEntity.created(location).body(profile);
    }

    /**
     * U9: edita una cuenta; el rol nunca cambia.
     *
     * @param id      id de la cuenta
     * @param request nuevos datos
     * @return 200 con el perfil actualizado
     */
    @PutMapping("/{id}")
    public UserProfile updateUser(@PathVariable Integer id, @Valid @RequestBody UpdateUserRequest request) {
        return administrationService.updateUser(id, request);
    }

    /**
     * U10: activa o desactiva una cuenta.
     *
     * @param id      id de la cuenta
     * @param request nuevo estado
     * @return 200 con el perfil actualizado
     */
    @PatchMapping("/{id}/status")
    public UserProfile updateUserStatus(@PathVariable Integer id, @Valid @RequestBody StatusRequest request) {
        return administrationService.changeStatus(currentUserService.current().userId(), id, request.status());
    }
}
