package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Obtiene el perfil específico de una cuenta según su rol: acudiente, entrenador o ninguno (administrador).
 *
 * <p>Lo usan el login (para los claims {@code guardianId}/{@code coachId}) y las consultas de perfil,
 * para que la regla "qué perfil corresponde a cada rol" esté en un solo lugar.</p>
 */
@Component
public class RoleProfileResolver {

    private final GuardianRepository guardianRepository;
    private final CoachRepository coachRepository;

    /**
     * @param guardianRepository perfiles de acudiente
     * @param coachRepository    perfiles de entrenador
     */
    public RoleProfileResolver(GuardianRepository guardianRepository, CoachRepository coachRepository) {
        this.guardianRepository = guardianRepository;
        this.coachRepository = coachRepository;
    }

    /**
     * @param user cuenta con su rol cargado
     * @return el perfil de acudiente o de entrenador de la cuenta; ambos vacíos para un administrador
     */
    public RoleProfiles resolve(User user) {
        return switch (user.getRole().getName()) {
            case GUARDIAN -> new RoleProfiles(guardianRepository.findByUserId(user.getId()).orElse(null), null);
            case COACH -> new RoleProfiles(null, coachRepository.findByUserId(user.getId()).orElse(null));
            case ADMIN -> new RoleProfiles(null, null);
        };
    }

    /**
     * Versión en bloque de {@link #resolve(User)} para listados: dos consultas en total, en lugar de
     * una por cuenta.
     *
     * @param users cuentas con su rol cargado
     * @return perfiles indexados por id de cuenta
     */
    public Map<Integer, RoleProfiles> resolveAll(Collection<User> users) {
        List<Integer> userIds = users.stream().map(User::getId).toList();
        Map<Integer, Guardian> guardians = guardianRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(guardian -> guardian.getUser().getId(), Function.identity()));
        Map<Integer, Coach> coaches = coachRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(coach -> coach.getUser().getId(), Function.identity()));
        return users.stream().collect(Collectors.toMap(User::getId,
                user -> new RoleProfiles(guardians.get(user.getId()), coaches.get(user.getId()))));
    }

    /**
     * @param guardian perfil de acudiente, o {@code null}
     * @param coach    perfil de entrenador, o {@code null}
     */
    public record RoleProfiles(Guardian guardian, Coach coach) {

        /**
         * @return id del perfil de acudiente, o {@code null}
         */
        public Integer guardianId() {
            return guardian == null ? null : guardian.getId();
        }

        /**
         * @return id del perfil de entrenador, o {@code null}
         */
        public Integer coachId() {
            return coach == null ? null : coach.getId();
        }
    }
}
