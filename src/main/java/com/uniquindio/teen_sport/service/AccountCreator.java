package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.EmailNormalizer;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea cuentas de usuario con las mismas reglas para todos los orígenes: registro público de
 * acudientes (U2), creación de personal por un administrador (U8) y administrador inicial.
 *
 * <p>Aplica la política de contraseñas, normaliza el correo, rechaza correo o documento ya
 * registrados y guarda solo el hash BCrypt. Debe invocarse dentro de la transacción del caso de uso.</p>
 */
@Component
public class AccountCreator {

    static final String EMAIL_TAKEN_MESSAGE = "El correo electrónico ya está registrado.";
    static final String ID_NUMBER_TAKEN_MESSAGE = "El número de documento ya está registrado.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    /**
     * @param userRepository  cuentas
     * @param roleRepository  catálogo de roles
     * @param passwordEncoder hash BCrypt
     * @param passwordPolicy  fortaleza de contraseñas
     * @param clock           reloj de la aplicación
     */
    public AccountCreator(UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    /**
     * Valida y guarda una cuenta activa.
     *
     * @param data datos de la cuenta
     * @return la cuenta guardada, con id asignado
     * @throws BusinessException 422 {@code WEAK_PASSWORD}; 409 {@code EMAIL_ALREADY_REGISTERED} o
     *                           {@code ID_NUMBER_ALREADY_REGISTERED}
     */
    public User create(NewAccount data) {
        passwordPolicy.validate(data.password());
        String email = EmailNormalizer.normalize(data.email());
        String idNumber = data.idNumber().trim();
        ensureNotRegistered(email, idNumber);

        User user = new User(role(data.role()), email, passwordEncoder.encode(data.password()), clock.instant());
        user.setDocumentType(data.idType().trim());
        user.setDocumentNumber(idNumber);
        user.setFirstName(data.firstNames().trim());
        user.setLastName(data.lastNames().trim());
        user.setPhone(TextNormalizer.trimToNull(data.phone()));
        return userRepository.save(user);
    }

    /**
     * @param email correo normalizado
     * @throws BusinessException 409 {@code EMAIL_ALREADY_REGISTERED} si el correo ya existe
     */
    public void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED, HttpStatus.CONFLICT, EMAIL_TAKEN_MESSAGE);
        }
    }

    private void ensureNotRegistered(String email, String idNumber) {
        ensureEmailAvailable(email);
        if (userRepository.existsByDocumentNumber(idNumber)) {
            throw new BusinessException(ErrorCode.ID_NUMBER_ALREADY_REGISTERED, HttpStatus.CONFLICT,
                    ID_NUMBER_TAKEN_MESSAGE);
        }
    }

    /**
     * Los roles se crean al arrancar ({@code ReferenceDataInitializer}); su ausencia es un fallo de
     * configuración, no un error del cliente.
     */
    private Role role(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("El rol " + name + " no está inicializado"));
    }

    /**
     * Datos de una cuenta nueva.
     *
     * @param role       rol
     * @param idType     tipo de documento
     * @param idNumber   número de documento
     * @param firstNames nombres
     * @param lastNames  apellidos
     * @param email      correo, sin normalizar
     * @param phone      teléfono, opcional
     * @param password   contraseña en claro
     */
    public record NewAccount(RoleName role, String idType, String idNumber, String firstNames, String lastNames,
                             String email, String phone, String password) {

        @Override
        public String toString() {
            return "NewAccount[role=" + role + ", email=" + email + ", password=****]";
        }
    }
}
