package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CoachListItem;
import com.uniquindio.teen_sport.dto.GuardianListItem;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.UserSearchSpecifications;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listados de apoyo para el administrador: entrenadores para asignar grupos (U11) y acudientes para
 * registrar deportistas en su nombre (U12). Devuelven solo los datos necesarios para elegir.
 */
@Service
public class UserDirectoryService {

    /** Orden de los listados: apellidos y nombres de la cuenta, y id para desempatar. */
    static final Sort ORDER_BY_USER_NAME = Sort.by("user.lastName", "user.firstName", "id");

    private final CoachRepository coachRepository;
    private final GuardianRepository guardianRepository;

    /**
     * @param coachRepository    perfiles de entrenador
     * @param guardianRepository perfiles de acudiente
     */
    public UserDirectoryService(CoachRepository coachRepository, GuardianRepository guardianRepository) {
        this.coachRepository = coachRepository;
        this.guardianRepository = guardianRepository;
    }

    /**
     * U11: entrenadores, opcionalmente filtrados por el estado de su cuenta.
     *
     * @param status estado, o {@code null} para todos
     * @return entrenadores ordenados por nombre
     */
    @Transactional(readOnly = true)
    public List<CoachListItem> listCoaches(EntityStatus status) {
        List<Coach> coaches = status == null
                ? coachRepository.findAllBy(ORDER_BY_USER_NAME)
                : coachRepository.findByUserStatus(status, ORDER_BY_USER_NAME);
        return coaches.stream().map(UserDirectoryService::toItem).toList();
    }

    /**
     * U12: acudientes paginados, con búsqueda libre opcional.
     *
     * @param text búsqueda libre sobre nombre, correo y documento, o {@code null}
     * @param page índice de página, desde 0
     * @param size tamaño de página
     * @return la página de acudientes
     */
    @Transactional(readOnly = true)
    public PageResponse<GuardianListItem> listGuardians(String text, int page, int size) {
        Page<Guardian> guardians = guardianRepository.findAll(UserSearchSpecifications.guardians(text),
                PageRequest.of(page, size, ORDER_BY_USER_NAME));
        return PageResponse.of(guardians, guardians.getContent().stream().map(UserDirectoryService::toItem).toList());
    }

    private static CoachListItem toItem(Coach coach) {
        User user = coach.getUser();
        return new CoachListItem(coach.getId(), user.getId(), fullName(user), coach.getSpecialty(),
                coach.getYearsExperience(), user.getStatus());
    }

    private static GuardianListItem toItem(Guardian guardian) {
        User user = guardian.getUser();
        return new GuardianListItem(guardian.getId(), user.getId(), fullName(user), user.getEmail(), user.getPhone());
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
