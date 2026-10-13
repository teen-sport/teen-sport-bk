package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.ChangePasswordRequest;
import com.uniquindio.teen_sport.dto.UpdateOwnProfileRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.SecurityMessages;
import com.uniquindio.teen_sport.mapper.UserProfileMapper;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la cuenta propia: consultar el perfil (U3), actualizarlo (U4) y cambiar la
 * contraseña (U5).
 *
 * <p>Reciben el id del usuario tomado del JWT, nunca del cuerpo de la solicitud. Las reglas de
 * edición de U4 están en {@link ProfileEditor}.</p>
 */
@Service
public class MyAccountService {

    static final String INCORRECT_PASSWORD_MESSAGE = "La contraseña actual no es correcta.";

    private final UserRepository userRepository;
    private final RoleProfileResolver roleProfileResolver;
    private final ProfileEditor profileEditor;
    private final UserProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    /**
     * @param userRepository      cuentas
     * @param roleProfileResolver perfil de acudiente o entrenador de cada cuenta
     * @param profileEditor       aplicación de los datos editables del perfil
     * @param profileMapper       salida pública del perfil
     * @param passwordEncoder     hash y verificación BCrypt
     * @param passwordPolicy      fortaleza de contraseñas
     */
    public MyAccountService(UserRepository userRepository, RoleProfileResolver roleProfileResolver,
                            ProfileEditor profileEditor, UserProfileMapper profileMapper,
                            PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.roleProfileResolver = roleProfileResolver;
        this.profileEditor = profileEditor;
        this.profileMapper = profileMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * U3: perfil del usuario autenticado.
     *
     * @param userId id del usuario autenticado
     * @return su perfil
     * @throws BusinessException 401 {@code UNAUTHENTICATED} si la cuenta del token ya no existe
     */
    @Transactional(readOnly = true)
    public UserProfile getProfile(Integer userId) {
        User user = findAccount(userId);
        return toProfile(user, roleProfileResolver.resolve(user));
    }

    /**
     * U4: actualiza los datos editables del perfil propio.
     *
     * @param userId  id del usuario autenticado
     * @param request nuevos datos
     * @return el perfil actualizado
     * @throws BusinessException 401 {@code UNAUTHENTICATED} si la cuenta del token ya no existe
     */
    @Transactional
    public UserProfile updateProfile(Integer userId, UpdateOwnProfileRequest request) {
        User user = findAccount(userId);
        RoleProfileResolver.RoleProfiles profiles = roleProfileResolver.resolve(user);
        profileEditor.apply(user, profiles, request);
        return toProfile(user, profiles);
    }

    /**
     * U5: cambia la contraseña propia tras confirmar la actual.
     *
     * <p>Los tokens ya emitidos siguen siendo válidos hasta su vencimiento (no hay revocación).</p>
     *
     * @param userId  id del usuario autenticado
     * @param request contraseña actual y nueva
     * @throws BusinessException 422 {@code INCORRECT_CURRENT_PASSWORD} o {@code WEAK_PASSWORD};
     *                           401 {@code UNAUTHENTICATED} si la cuenta del token ya no existe
     */
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = findAccount(userId);
        if (PasswordPolicy.exceedsMaxBytes(request.currentPassword())
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INCORRECT_CURRENT_PASSWORD, HttpStatus.UNPROCESSABLE_CONTENT,
                    INCORRECT_PASSWORD_MESSAGE);
        }
        passwordPolicy.validate(request.newPassword());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * Un token válido cuya cuenta ya no existe no identifica a nadie: se trata como no autenticado.
     */
    private User findAccount(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED,
                        SecurityMessages.UNAUTHENTICATED));
    }

    private UserProfile toProfile(User user, RoleProfileResolver.RoleProfiles profiles) {
        return profileMapper.toProfile(user, profiles.guardian(), profiles.coach());
    }
}
