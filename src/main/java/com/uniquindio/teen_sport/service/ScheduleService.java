package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.ScheduleRequest;
import com.uniquindio.teen_sport.dto.ScheduleResponse;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.GroupMapper;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.validation.TimeSlot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de las franjas semanales: crear (H12), editar (H13) y eliminar (H14).
 *
 * <p>Una franja debe terminar después de empezar y no cruzarse con otra del grupo ni con las de los
 * otros grupos activos del entrenador ({@link ScheduleConflictChecker}). Se pueden gestionar franjas
 * de grupos inactivos. Al crear o editar se bloquean el grupo y su entrenador hasta el final de la
 * transacción, de modo que dos cambios simultáneos no puedan dejar franjas cruzadas.</p>
 */
@Service
public class ScheduleService {

    static final String GROUP_NOT_FOUND_MESSAGE = "El grupo no existe.";
    static final String SCHEDULE_NOT_FOUND_MESSAGE = "La franja no existe.";
    static final String INVALID_TIME_RANGE_MESSAGE = "La hora de fin debe ser posterior a la hora de inicio.";

    private final ScheduleRepository scheduleRepository;
    private final GroupRepository groupRepository;
    private final CoachRepository coachRepository;
    private final ScheduleConflictChecker conflictChecker;

    /**
     * @param scheduleRepository franjas
     * @param groupRepository    grupos
     * @param coachRepository    entrenadores
     * @param conflictChecker    detección de cruces
     */
    public ScheduleService(ScheduleRepository scheduleRepository, GroupRepository groupRepository,
                           CoachRepository coachRepository, ScheduleConflictChecker conflictChecker) {
        this.scheduleRepository = scheduleRepository;
        this.groupRepository = groupRepository;
        this.coachRepository = coachRepository;
        this.conflictChecker = conflictChecker;
    }

    /**
     * H12: agrega una franja al grupo.
     *
     * @param groupId id del grupo
     * @param request día y horas
     * @return la franja creada
     * @throws BusinessException 404 {@code NOT_FOUND}; 422 {@code INVALID_TIME_RANGE}; 409 {@code SCHEDULE_CONFLICT}
     */
    @Transactional
    public ScheduleResponse create(Integer groupId, ScheduleRequest request) {
        Group group = lockGroupAndCoach(groupId);
        TimeSlot slot = validSlot(request);
        conflictChecker.requireNoConflict(group, slot, null);
        Schedule schedule = new Schedule(group);
        apply(schedule, slot);
        return GroupMapper.toResponse(scheduleRepository.save(schedule));
    }

    /**
     * H13: edita una franja, con las mismas validaciones que H12.
     *
     * @param scheduleId id de la franja
     * @param request    día y horas
     * @return la franja actualizada
     * @throws BusinessException 404 {@code NOT_FOUND}; 422 {@code INVALID_TIME_RANGE}; 409 {@code SCHEDULE_CONFLICT}
     */
    @Transactional
    public ScheduleResponse update(Integer scheduleId, ScheduleRequest request) {
        Schedule schedule = findSchedule(scheduleId);
        Group group = lockGroupAndCoach(schedule.getGroup().getId());
        TimeSlot slot = validSlot(request);
        conflictChecker.requireNoConflict(group, slot, scheduleId);
        apply(schedule, slot);
        return GroupMapper.toResponse(schedule);
    }

    /**
     * H14: elimina una franja. Las sesiones ya creadas a partir de ella no se eliminan.
     *
     * @param scheduleId id de la franja
     * @throws BusinessException 404 {@code NOT_FOUND}
     */
    @Transactional
    public void delete(Integer scheduleId) {
        scheduleRepository.delete(findSchedule(scheduleId));
    }

    private Group lockGroupAndCoach(Integer groupId) {
        Group group = groupRepository.findLockedById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, GROUP_NOT_FOUND_MESSAGE));
        coachRepository.findLockedById(group.getCoach().getId());
        return group;
    }

    private Schedule findSchedule(Integer scheduleId) {
        return scheduleRepository.findWithGroupById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        SCHEDULE_NOT_FOUND_MESSAGE));
    }

    private static TimeSlot validSlot(ScheduleRequest request) {
        TimeSlot slot = new TimeSlot(request.dayOfWeek(), request.startTime(), request.endTime());
        if (!slot.hasValidRange()) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE, HttpStatus.UNPROCESSABLE_CONTENT,
                    INVALID_TIME_RANGE_MESSAGE);
        }
        return slot;
    }

    private static void apply(Schedule schedule, TimeSlot slot) {
        schedule.setDayOfWeek(slot.day());
        schedule.setStartTime(slot.start());
        schedule.setEndTime(slot.end());
    }
}
