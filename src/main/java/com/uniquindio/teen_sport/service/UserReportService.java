package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.UserReport;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.RoleCount;
import com.uniquindio.teen_sport.repository.RoleStatusCount;
import com.uniquindio.teen_sport.repository.UserRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Informe de cuentas de usuario (R5), con los KPI del Proceso 3: cuentas registradas por rol y
 * periodo, porcentaje de acudientes con deportistas vinculados y número de cuentas inactivas.
 *
 * <p>Reglas (decisión del 2026-11-10):</p>
 * <ul>
 *   <li>Las cuentas registradas en el periodo son las creadas ese mes en la zona horaria de la academia,
 *       aunque hoy estén inactivas.</li>
 *   <li>Las cuentas activas, las inactivas y la vinculación de acudientes reflejan el estado actual,
 *       porque el estado de las cuentas no guarda historia.</li>
 *   <li>{@code guardianLinkRate} = acudientes activos con al menos un deportista / acudientes activos;
 *       sin acudientes activos vale 0.</li>
 * </ul>
 */
@Service
public class UserReportService {

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final AcademyCalendar calendar;

    /**
     * @param userRepository     cuentas de usuario
     * @param guardianRepository perfiles de acudiente
     * @param calendar           zona horaria de la academia, para los límites del periodo
     */
    public UserReportService(UserRepository userRepository, GuardianRepository guardianRepository,
                             AcademyCalendar calendar) {
        this.userRepository = userRepository;
        this.guardianRepository = guardianRepository;
        this.calendar = calendar;
    }

    /**
     * R5: indicadores de cuentas.
     *
     * @param period mes de creación de las cuentas contadas como registradas
     * @return el informe
     */
    @Transactional(readOnly = true)
    public UserReport userReport(YearMonth period) {
        ZoneId zone = calendar.zone();
        Instant from = period.atDay(1).atStartOfDay(zone).toInstant();
        Instant to = period.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        Map<RoleName, Long> registered = registeredByRole(userRepository.countCreatedByRole(from, to));
        Map<RoleName, Map<EntityStatus, Long>> byStatus = byRoleAndStatus(userRepository.countByRoleAndStatus());

        List<UserReport.RoleAccounts> byRole = Arrays.stream(RoleName.values())
                .map(role -> new UserReport.RoleAccounts(role, registered.get(role),
                        byStatus.get(role).get(EntityStatus.ACTIVE), byStatus.get(role).get(EntityStatus.INACTIVE)))
                .toList();
        long activeGuardians = byStatus.get(RoleName.GUARDIAN).get(EntityStatus.ACTIVE);
        long guardiansWithAthletes = guardianRepository.countActiveWithAthletes();
        return new UserReport(period,
                byRole.stream().mapToLong(UserReport.RoleAccounts::registeredInPeriod).sum(),
                byRole.stream().mapToLong(UserReport.RoleAccounts::inactive).sum(),
                guardiansWithAthletes, Percentages.of(guardiansWithAthletes, activeGuardians), byRole);
    }

    private static Map<RoleName, Long> registeredByRole(List<RoleCount> counts) {
        Map<RoleName, Long> byRole = new EnumMap<>(RoleName.class);
        for (RoleName role : RoleName.values()) {
            byRole.put(role, 0L);
        }
        counts.forEach(count -> byRole.put(count.getRole(), count.getTotal()));
        return byRole;
    }

    private static Map<RoleName, Map<EntityStatus, Long>> byRoleAndStatus(List<RoleStatusCount> counts) {
        Map<RoleName, Map<EntityStatus, Long>> byRole = new EnumMap<>(RoleName.class);
        for (RoleName role : RoleName.values()) {
            Map<EntityStatus, Long> byStatus = new EnumMap<>(EntityStatus.class);
            for (EntityStatus status : EntityStatus.values()) {
                byStatus.put(status, 0L);
            }
            byRole.put(role, byStatus);
        }
        counts.forEach(count -> byRole.get(count.getRole()).put(count.getStatus(), count.getTotal()));
        return byRole;
    }
}
