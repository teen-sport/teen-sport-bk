package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.validation.TimeSlot;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Detecta cruces de franjas semanales ({@link TimeSlot#overlaps(TimeSlot)}).
 *
 * <p>Una franja de un grupo no puede cruzarse con otra del mismo grupo ni con las de los demás
 * grupos <b>activos</b> de su entrenador: los grupos inactivos no entrenan y no bloquean al
 * entrenador. Quien llama debe tener bloqueados el grupo y el entrenador para que la comprobación
 * siga siendo válida al guardar.</p>
 */
@Component
public class ScheduleConflictChecker {

    static final String SCHEDULE_CONFLICT_MESSAGE =
            "La franja se cruza con otra del grupo o con otra franja del entrenador.";
    static final String COACH_CONFLICT_MESSAGE =
            "El entrenador tiene otro grupo activo en un horario que se cruza con los de este grupo.";

    private final ScheduleRepository scheduleRepository;

    /**
     * @param scheduleRepository franjas guardadas
     */
    public ScheduleConflictChecker(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * H12/H13: comprueba que una franja nueva o editada no se cruce.
     *
     * @param group              grupo de la franja
     * @param slot               franja propuesta
     * @param excludedScheduleId franja que se está editando (se excluye), o {@code null} al crear
     * @throws BusinessException 409 {@code SCHEDULE_CONFLICT}
     */
    public void requireNoConflict(Group group, TimeSlot slot, Integer excludedScheduleId) {
        boolean conflict = overlapsAny(slot, scheduleRepository.findByGroupId(group.getId()), excludedScheduleId)
                || overlapsAny(slot, scheduleRepository.findActiveByCoachExcludingGroup(
                        group.getCoach().getId(), group.getId()), null);
        if (conflict) {
            throw new BusinessException(ErrorCode.SCHEDULE_CONFLICT, HttpStatus.CONFLICT, SCHEDULE_CONFLICT_MESSAGE);
        }
    }

    /**
     * H11: comprueba que el entrenador pueda asumir todas las franjas del grupo.
     *
     * @param group   grupo que recibirá al entrenador
     * @param coachId entrenador propuesto
     * @throws BusinessException 409 {@code COACH_SCHEDULE_CONFLICT}
     */
    public void requireCoachAvailable(Group group, Integer coachId) {
        List<Schedule> coachSchedules = scheduleRepository.findActiveByCoachExcludingGroup(coachId, group.getId());
        boolean conflict = scheduleRepository.findByGroupId(group.getId()).stream()
                .anyMatch(schedule -> overlapsAny(TimeSlot.of(schedule), coachSchedules, null));
        if (conflict) {
            throw new BusinessException(ErrorCode.COACH_SCHEDULE_CONFLICT, HttpStatus.CONFLICT, COACH_CONFLICT_MESSAGE);
        }
    }

    private static boolean overlapsAny(TimeSlot slot, List<Schedule> schedules, Integer excludedScheduleId) {
        return schedules.stream()
                .filter(schedule -> !Objects.equals(schedule.getId(), excludedScheduleId))
                .anyMatch(schedule -> slot.overlaps(TimeSlot.of(schedule)));
    }
}
