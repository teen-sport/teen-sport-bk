package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.User;
import org.springframework.stereotype.Component;

/**
 * Convierte una cuenta y su perfil específico en el {@link UserProfile} del contrato.
 *
 * <p>Es la única salida de datos de usuario hacia la API: copia campos explícitamente para que
 * el hash de la contraseña nunca llegue a una respuesta.</p>
 */
@Component
public class UserProfileMapper {

    /**
     * @param user     cuenta con su rol cargado
     * @param guardian perfil de acudiente, o {@code null} si la cuenta no es de acudiente
     * @param coach    perfil de entrenador, o {@code null} si la cuenta no es de entrenador
     * @return perfil público de la cuenta
     */
    public UserProfile toProfile(User user, Guardian guardian, Coach coach) {
        return new UserProfile(
                user.getId(),
                user.getDocumentType(),
                user.getDocumentNumber(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getName(),
                user.getStatus(),
                user.isAcceptedDataPolicy(),
                guardian == null ? null : new UserProfile.GuardianData(guardian.getId(), guardian.getAddress()),
                coach == null ? null
                        : new UserProfile.CoachData(coach.getId(), coach.getSpecialty(), coach.getYearsExperience()));
    }
}
