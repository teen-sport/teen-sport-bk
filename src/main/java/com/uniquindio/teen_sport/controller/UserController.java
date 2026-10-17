package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.ChangePasswordRequest;
import com.uniquindio.teen_sport.dto.RegisterGuardianRequest;
import com.uniquindio.teen_sport.dto.UpdateOwnProfileRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.GuardianRegistrationService;
import com.uniquindio.teen_sport.service.MyAccountService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Usuarios (tag {@code Users} del contrato).
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final GuardianRegistrationService registrationService;
    private final MyAccountService myAccountService;
    private final CurrentUserService currentUserService;

    /**
     * @param registrationService caso de uso de registro de acudientes
     * @param myAccountService    casos de uso de la cuenta propia
     * @param currentUserService  identidad del usuario autenticado, tomada del JWT
     */
    public UserController(GuardianRegistrationService registrationService, MyAccountService myAccountService,
                          CurrentUserService currentUserService) {
        this.registrationService = registrationService;
        this.myAccountService = myAccountService;
        this.currentUserService = currentUserService;
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

    /**
     * U3: perfil del usuario autenticado. Cualquier rol.
     *
     * @return 200 con el perfil
     */
    @GetMapping("/me")
    public UserProfile getMyProfile() {
        return myAccountService.getProfile(currentUserService.current().userId());
    }

    /**
     * U4: actualiza el perfil propio. Cualquier rol; correo, documento y rol no se modifican.
     *
     * @param request nuevos datos
     * @return 200 con el perfil actualizado
     */
    @PutMapping("/me")
    public UserProfile updateMyProfile(@Valid @RequestBody UpdateOwnProfileRequest request) {
        return myAccountService.updateProfile(currentUserService.current().userId(), request);
    }

    /**
     * U5: cambia la contraseña propia. Cualquier rol.
     *
     * @param request contraseña actual y nueva
     */
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        myAccountService.changePassword(currentUserService.current().userId(), request);
    }
}
