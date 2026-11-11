package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.LoginRequest;
import com.uniquindio.teen_sport.dto.LoginResponse;
import com.uniquindio.teen_sport.dto.PasswordResetConfirmRequest;
import com.uniquindio.teen_sport.dto.PasswordResetRequest;
import com.uniquindio.teen_sport.service.AuthService;
import com.uniquindio.teen_sport.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación y recuperación de contraseña (tag {@code Auth} del contrato).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * @param authService          caso de uso de login
     * @param passwordResetService recuperación de contraseña
     */
    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * U1: inicia sesión y devuelve un JWT. Público.
     *
     * @param request credenciales
     * @return 200 con el token y los datos mínimos del usuario
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * U13: pide el enlace de recuperación. Público. Responde igual exista o no el correo.
     *
     * @param request correo de la cuenta
     */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
    }

    /**
     * U14: restablece la contraseña con el token del correo. Público.
     *
     * @param request token y nueva contraseña
     */
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
    }
}
