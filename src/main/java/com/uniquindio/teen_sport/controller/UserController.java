package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.RegisterGuardianRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.service.GuardianRegistrationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Usuarios (tag {@code Users} del contrato).
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final GuardianRegistrationService registrationService;

    /**
     * @param registrationService caso de uso de registro de acudientes
     */
    public UserController(GuardianRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * U2: registro público de un acudiente.
     *
     * @param request datos del acudiente
     * @return 201 con el perfil creado y la cabecera {@code Location} del usuario
     */
    @PostMapping("/register")
    public ResponseEntity<UserProfile> register(@Valid @RequestBody RegisterGuardianRequest request) {
        UserProfile profile = registrationService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(WebConfig.API_PREFIX + "/users/{id}")
                .buildAndExpand(profile.id())
                .toUri();
        return ResponseEntity.created(location).body(profile);
    }
}
