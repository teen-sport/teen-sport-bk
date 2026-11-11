package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.RegisterGuardianRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.UserProfileMapper;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso U2: registro público de un acudiente.
 *
 * <p>Crea la cuenta ({@code users}, rol {@code GUARDIAN}) y su perfil ({@code guardians}) en una
 * sola transacción. Exige aceptar la política de tratamiento de datos (Ley 1581 de 2012) y guarda
 * el momento de la aceptación. Al confirmarse, el acudiente recibe el correo de bienvenida
 * {@code ACCOUNT_REGISTERED}.</p>
 */
@Service
public class GuardianRegistrationService {

    static final String DATA_POLICY_MESSAGE = "Debes aceptar la política de tratamiento de datos personales.";

    private final AccountCreator accountCreator;
    private final GuardianRepository guardianRepository;
    private final UserProfileMapper profileMapper;
    private final Clock clock;
    private final NotificationService notificationService;

    /**
     * @param accountCreator     creación de cuentas con las reglas comunes
     * @param guardianRepository perfiles de acudiente
     * @param profileMapper      salida pública del perfil
     * @param clock               reloj de la aplicación
     * @param notificationService correo de bienvenida
     */
    public GuardianRegistrationService(AccountCreator accountCreator, GuardianRepository guardianRepository,
                                       UserProfileMapper profileMapper, Clock clock,
                                       NotificationService notificationService) {
        this.accountCreator = accountCreator;
        this.guardianRepository = guardianRepository;
        this.profileMapper = profileMapper;
        this.clock = clock;
        this.notificationService = notificationService;
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
        User user = accountCreator.create(new AccountCreator.NewAccount(RoleName.GUARDIAN, request.idType(),
                request.idNumber(), request.firstNames(), request.lastNames(), request.email(), request.phone(),
                request.password()));
        user.acceptDataPolicy(clock.instant());

        Guardian guardian = guardianRepository.save(
                new Guardian(user, TextNormalizer.trimToNull(request.homeAddress())));
        notificationService.accountRegistered(user);
        return profileMapper.toProfile(user, guardian, null);
    }
}
