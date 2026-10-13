package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.LoginRequest;
import com.uniquindio.teen_sport.dto.LoginResponse;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.security.JwtTokenService;
import com.uniquindio.teen_sport.validation.EmailNormalizer;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso U1: autenticar por correo y contraseña y emitir el JWT de acceso.
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>Correo inexistente y contraseña incorrecta responden el mismo 401 {@code INVALID_CREDENTIALS}.
 *       Si el correo no existe se verifica igualmente contra un hash ficticio, para que el tiempo de
 *       respuesta no revele qué correos están registrados.</li>
 *   <li>Una cuenta inactiva responde 403 {@code USER_INACTIVE}, solo después de verificar la
 *       contraseña, para no revelar el estado de cuentas ajenas.</li>
 * </ul>
 */
@Service
public class AuthService {

    static final String TOKEN_TYPE = "Bearer";
    static final String INVALID_CREDENTIALS_MESSAGE = "Correo o contraseña incorrectos.";
    static final String USER_INACTIVE_MESSAGE = "Tu cuenta está inactiva. Comunícate con la academia.";

    private final UserRepository userRepository;
    private final RoleProfileResolver roleProfileResolver;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final String dummyPasswordHash;

    /**
     * @param userRepository      cuentas
     * @param roleProfileResolver perfil de acudiente o entrenador de cada cuenta
     * @param passwordEncoder     verificación BCrypt
     * @param tokenService        emisión de JWT
     */
    public AuthService(UserRepository userRepository, RoleProfileResolver roleProfileResolver,
                       PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userRepository = userRepository;
        this.roleProfileResolver = roleProfileResolver;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * @param request credenciales
     * @return token de acceso y datos mínimos del usuario
     * @throws BusinessException 401 {@code INVALID_CREDENTIALS} o 403 {@code USER_INACTIVE}
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> account = userRepository.findByEmail(EmailNormalizer.normalize(request.email()));
        if (!passwordMatches(request.password(), account) || account.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                    INVALID_CREDENTIALS_MESSAGE);
        }
        User user = account.get();
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, HttpStatus.FORBIDDEN, USER_INACTIVE_MESSAGE);
        }
        AuthenticatedUser identity = identityOf(user);
        return new LoginResponse(tokenService.issue(identity), TOKEN_TYPE, tokenService.expirationSeconds(),
                new LoginResponse.LoggedUser(user.getId(), user.getFirstName(), user.getLastName(),
                        identity.role(), identity.guardianId(), identity.coachId()));
    }

    /**
     * Ninguna contraseña guardada supera el límite de BCrypt, así que una más larga nunca coincide.
     */
    private boolean passwordMatches(String rawPassword, Optional<User> account) {
        String hash = account.map(User::getPasswordHash).orElse(dummyPasswordHash);
        if (PasswordPolicy.exceedsMaxBytes(rawPassword)) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, hash);
    }

    private AuthenticatedUser identityOf(User user) {
        RoleProfileResolver.RoleProfiles profiles = roleProfileResolver.resolve(user);
        return new AuthenticatedUser(user.getId(), user.getRole().getName(), profiles.guardianId(),
                profiles.coachId());
    }
}
