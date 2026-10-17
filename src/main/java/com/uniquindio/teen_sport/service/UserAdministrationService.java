package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CreateUserRequest;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.UpdateUserRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.UserProfileMapper;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.repository.UserSearchSpecifications;
import com.uniquindio.teen_sport.validation.EmailNormalizer;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de administración de cuentas, reservados al rol {@code ADMIN}: listar (U6), consultar
 * (U7), crear personal (U8), editar (U9) y activar o desactivar (U10).
 *
 * <p>El rol de una cuenta nunca cambia. La edición sigue las reglas de {@link ProfileEditor} y,
 * además, permite cambiar el correo.</p>
 */
@Service
public class UserAdministrationService {

    static final String USER_NOT_FOUND_MESSAGE = "El usuario no existe.";
    static final String CANNOT_DEACTIVATE_SELF_MESSAGE = "No puedes desactivar tu propia cuenta.";
    static final String COACH_HAS_ACTIVE_GROUPS_MESSAGE =
            "El entrenador tiene grupos activos. Reasígnalos o desactívalos antes de desactivar su cuenta.";

    /** Orden de los listados: apellidos, nombres y, para desempatar, id. */
    static final Sort USER_ORDER = Sort.by("lastName", "firstName", "id");

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final GroupRepository groupRepository;
    private final AccountCreator accountCreator;
    private final RoleProfileResolver roleProfileResolver;
    private final ProfileEditor profileEditor;
    private final UserProfileMapper profileMapper;

    /**
     * @param userRepository      cuentas
     * @param coachRepository     perfiles de entrenador
     * @param groupRepository     grupos de entrenamiento
     * @param accountCreator      creación de cuentas con las reglas comunes
     * @param roleProfileResolver perfil de acudiente o entrenador de cada cuenta
     * @param profileEditor       aplicación de los datos editables del perfil
     * @param profileMapper       salida pública del perfil
     */
    public UserAdministrationService(UserRepository userRepository, CoachRepository coachRepository,
                                     GroupRepository groupRepository, AccountCreator accountCreator, RoleProfileResolver roleProfileResolver,
                                     ProfileEditor profileEditor, UserProfileMapper profileMapper) {
        this.userRepository = userRepository;
        this.coachRepository = coachRepository;
        this.groupRepository = groupRepository;
        this.accountCreator = accountCreator;
        this.roleProfileResolver = roleProfileResolver;
        this.profileEditor = profileEditor;
        this.profileMapper = profileMapper;
    }

    /**
     * U6: listado paginado con filtros opcionales.
     *
     * @param role   rol, o {@code null}
     * @param status estado, o {@code null}
     * @param text   búsqueda libre sobre nombre, correo y documento, o {@code null}
     * @param page   índice de página, desde 0
     * @param size   tamaño de página
     * @return la página de perfiles
     */
    @Transactional(readOnly = true)
    public PageResponse<UserProfile> listUsers(RoleName role, EntityStatus status, String text, int page, int size) {
        Page<User> users = userRepository.findAll(UserSearchSpecifications.users(role, status, text),
                PageRequest.of(page, size, USER_ORDER));
        Map<Integer, RoleProfileResolver.RoleProfiles> profiles = roleProfileResolver.resolveAll(users.getContent());
        return PageResponse.of(users, users.getContent().stream()
                .map(user -> toProfile(user, profiles.get(user.getId())))
                .toList());
    }

    /**
     * U7: detalle de una cuenta.
     *
     * @param userId id de la cuenta
     * @return su perfil
     * @throws BusinessException 404 {@code NOT_FOUND}
     */
    @Transactional(readOnly = true)
    public UserProfile getUser(Integer userId) {
        User user = findUser(userId);
        return toProfile(user, roleProfileResolver.resolve(user));
    }

    /**
     * U8: crea una cuenta de administrador o de entrenador; para un entrenador crea también su perfil.
     *
     * @param request datos de la cuenta
     * @return el perfil creado
     * @throws BusinessException 422 {@code WEAK_PASSWORD}; 409 {@code EMAIL_ALREADY_REGISTERED} o
     *                           {@code ID_NUMBER_ALREADY_REGISTERED}
     */
    @Transactional
    public UserProfile createStaffUser(CreateUserRequest request) {
        User user = accountCreator.create(new AccountCreator.NewAccount(request.role(), request.idType(),
                request.idNumber(), request.firstNames(), request.lastNames(), request.email(), request.phone(),
                request.password()));
        Coach coach = null;
        if (request.role() == RoleName.COACH) {
            coach = coachRepository.save(new Coach(user, TextNormalizer.trimToNull(request.specialty()),
                    request.yearsOfExperience()));
        }
        return profileMapper.toProfile(user, null, coach);
    }

    /**
     * U9: edita los datos de una cuenta. El correo solo cambia si se envía uno distinto.
     *
     * @param userId  id de la cuenta
     * @param request nuevos datos
     * @return el perfil actualizado
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code EMAIL_ALREADY_REGISTERED}
     */
    @Transactional
    public UserProfile updateUser(Integer userId, UpdateUserRequest request) {
        User user = findUser(userId);
        String email = TextNormalizer.trimToNull(request.email());
        if (email != null) {
            String normalized = EmailNormalizer.normalize(email);
            if (!normalized.equals(user.getEmail())) {
                accountCreator.ensureEmailAvailable(normalized);
                user.changeEmail(normalized);
            }
        }
        RoleProfileResolver.RoleProfiles profiles = roleProfileResolver.resolve(user);
        profileEditor.apply(user, profiles, request);
        return toProfile(user, profiles);
    }

    /**
     * U10: activa o desactiva una cuenta. Una cuenta inactiva no puede iniciar sesión; los tokens ya
     * emitidos vencen por sí solos.
     *
     * <p>No se puede desactivar a un entrenador que tenga grupos activos: primero hay que reasignarlos
     * (H11) o desactivarlos (H10).</p>
     *
     * @param actingUserId id del administrador que hace el cambio
     * @param userId       id de la cuenta
     * @param status       nuevo estado
     * @return el perfil actualizado
     * @throws BusinessException 404 {@code NOT_FOUND}; 422 {@code CANNOT_DEACTIVATE_SELF};
     *                           409 {@code COACH_HAS_ACTIVE_GROUPS}
     */
    @Transactional
    public UserProfile changeStatus(Integer actingUserId, Integer userId, EntityStatus status) {
        User user = findUser(userId);
        if (status == EntityStatus.INACTIVE && user.getId().equals(actingUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_DEACTIVATE_SELF, HttpStatus.UNPROCESSABLE_CONTENT,
                    CANNOT_DEACTIVATE_SELF_MESSAGE);
        }
        RoleProfileResolver.RoleProfiles profiles = roleProfileResolver.resolve(user);
        if (status == EntityStatus.INACTIVE && profiles.coach() != null
                && groupRepository.existsByCoachIdAndStatus(profiles.coachId(), EntityStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.COACH_HAS_ACTIVE_GROUPS, HttpStatus.CONFLICT,
                    COACH_HAS_ACTIVE_GROUPS_MESSAGE);
        }
        user.setStatus(status);
        return toProfile(user, profiles);
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        USER_NOT_FOUND_MESSAGE));
    }

    private UserProfile toProfile(User user, RoleProfileResolver.RoleProfiles profiles) {
        return profileMapper.toProfile(user, profiles.guardian(), profiles.coach());
    }
}
