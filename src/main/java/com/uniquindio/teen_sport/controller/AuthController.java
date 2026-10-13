package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.LoginRequest;
import com.uniquindio.teen_sport.dto.LoginResponse;
import com.uniquindio.teen_sport.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación (tag {@code Auth} del contrato).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * @param authService caso de uso de login
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
