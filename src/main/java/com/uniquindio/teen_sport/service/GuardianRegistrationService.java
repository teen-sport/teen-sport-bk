package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.RegisterGuardianRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.UserProfileMapper;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.EmailNormalizer;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso U2: registro público de un acudiente.
 *
 * <p>Crea la cuenta ({@code users}, rol {@code GUARDIAN}) y su perfil ({@code guardians}) en una
 * sola transacción. Exige aceptar la política de tratamiento de datos (Ley 1581 de 2012) y guarda
 * el momento de la aceptación.</p>
 */
@Service
public class GuardianRegistrationService {

    static final String DATA_POLICY_MESSAGE = "Debes aceptar la política de tratamiento de datos personales.";
    static final String EMAIL_TAKEN_MESSAGE = "El correo electrónico ya está registrado.";
    static final String ID_NUMBER_TAKEN_MESSAGE = "El número de documento ya está registrado.";

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final UserProfileMapper profileMapper;
    private final Clock clock;

    /**
     * @param userRepository     cuentas
     * @param guardianRepository perfiles de acudiente
     * @param roleRepository     catálogo de roles
     * @param passwordEncoder    hash BCrypt
     * @param passwordPolicy     fortaleza de contraseñas
     * @param profileMapper      salida pública del perfil
     * @param clock              reloj de la aplicación
     */
    public GuardianRegistrationService(UserRepository userRepository, GuardianRepository guardianRepository,
                                       RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                                       PasswordPolicy passwordPolicy, UserProfileMapper profileMapper, Clock clock) {
        this.userRepository = userRepository;
        this.guardianRepository = guardianRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.profileMapper = profileMapper;
        this.clock = clock;
    }

    /**
     * @param request datos del registro, ya validados en formato
     * @return perfil del acudiente creado
     * @throws BusinessException 422 {@code DATA_POLICY_NOT_ACCEPTED} o {@code WEAK_PASSWORD};
     *                           409 {@code EMAIL_ALREADY_REGISTERED} o {@code ID_NUMBER_ALREADY_REGISTERED}
     */
    @Transactional
    public UserProfile register(RegisterGuardianRequest request) {
        if (!Boolean.TRUE.equals(request.acceptedDataPolicy())) {
            throw new BusinessException(ErrorCode.DATA_POLICY_NOT_ACCEPTED, HttpStatus.UNPROCESSABLE_CONTENT,
                    DATA_POLICY_MESSAGE);
        }
        passwordPolicy.validate(request.password());
        String email = EmailNormalizer.normalize(request.email());
        String idNumber = request.idNumber().trim();
        ensureNotRegistered(email, idNumber);

        Instant now = clock.instant();
        User user = new User(guardianRole(), email, passwordEncoder.encode(request.password()), now);
        user.setDocumentType(request.idType().trim());
        user.setDocumentNumber(idNumber);
        user.setFirstName(request.firstNames().trim());
        user.setLastName(request.lastNames().trim());
        user.setPhone(request.phone().trim());
        user.acceptDataPolicy(now);
        userRepository.save(user);

        Guardian guardian = guardianRepository.save(new Guardian(user, blankToNull(request.homeAddress())));
        return profileMapper.toProfile(user, guardian, null);
    }

    private void ensureNotRegistered(String email, String idNumber) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED, HttpStatus.CONFLICT, EMAIL_TAKEN_MESSAGE);
        }
        if (userRepository.existsByDocumentNumber(idNumber)) {
            throw new BusinessException(ErrorCode.ID_NUMBER_ALREADY_REGISTERED, HttpStatus.CONFLICT,
                    ID_NUMBER_TAKEN_MESSAGE);
        }
    }

    /**
     * Los roles se crean al arrancar ({@code ReferenceDataInitializer}); su ausencia es un fallo de
     * configuración, no un error del cliente.
     */
    private Role guardianRole() {
        return roleRepository.findByName(RoleName.GUARDIAN)
                .orElseThrow(() -> new IllegalStateException("El rol GUARDIAN no está inicializado"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
