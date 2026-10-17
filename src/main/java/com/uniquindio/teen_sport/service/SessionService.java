package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CreateSessionRequest;
import com.uniquindio.teen_sport.dto.GenerateSessionsRequest;
import com.uniquindio.teen_sport.dto.GenerateSessionsResponse;
import com.uniquindio.teen_sport.dto.SessionResponse;
import com.uniquindio.teen_sport.dto.UpdateSessionRequest;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.SessionMapper;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.DateRangePolicy;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Programación de sesiones: crear (A1), generar desde las franjas (A2), reprogramar (A5) y cancelar (A6).
 * El administrador gestiona todas; un entrenador, solo las de los grupos que tiene a cargo
 * ({@link GroupAccessPolicy}).
 *
 * <p>Crear, generar y reprogramar bloquean el grupo antes de comprobar duplicados, para que dos
 * solicitudes simultáneas no dejen dos sesiones con la misma fecha y hora de inicio. Reprogramar bloquea
 * después la sesión; cancelar solo la sesión. El orden grupo y luego sesión es el mismo de H11, que
 * traspasa las sesiones futuras bajo el bloqueo del grupo.</p>
 *
 * <p>Los correos {@code SCHEDULE_PUBLISHED} (A2 con {@code notify}) y {@code SCHEDULE_CHANGE} (A5, A6)
 * llegan con el adaptador de correo (Fase 7).</p>
 */
@Service
public class SessionService {

    static final String GROUP_NOT_FOUND_MESSAGE = "El grupo no existe.";
    static final String SESSION_NOT_FOUND_MESSAGE = "La sesión no existe.";
    static final String GROUP_INACTIVE_MESSAGE = "El grupo está inactivo.";
    static final String WITHOUT_SCHEDULES_MESSAGE = "El grupo no tiene franjas semanales.";
    static final String NOT_SCHEDULED_MESSAGE = "Solo se puede modificar una sesión programada.";

    private final GroupRepository groupRepository;
    private final ScheduleRepository scheduleRepository;
    private final TrainingSessionRepository sessionRepository;
    private final GroupAccessPolicy accessPolicy;
    private final SessionSlotPolicy slotPolicy;

    /**
     * @param groupRepository    grupos
     * @param scheduleRepository franjas
     * @param sessionRepository  sesiones
     * @param accessPolicy       regla de propiedad de los grupos
     * @param slotPolicy         reglas de fecha y hora
     */
    public SessionService(GroupRepository groupRepository, ScheduleRepository scheduleRepository,
                          TrainingSessionRepository sessionRepository, GroupAccessPolicy accessPolicy,
                          SessionSlotPolicy slotPolicy) {
        this.groupRepository = groupRepository;
        this.scheduleRepository = scheduleRepository;
        this.sessionRepository = sessionRepository;
        this.accessPolicy = accessPolicy;
        this.slotPolicy = slotPolicy;
    }

    /**
     * A1: crea una sesión programada, a cargo del entrenador actual del grupo. No tiene que coincidir con
     * una franja del grupo.
     *
     * @param user    identidad autenticada
     * @param request datos de la sesión
     * @return la sesión creada
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED}; 422 {@code GROUP_INACTIVE},
     *                           {@code INVALID_TIME_RANGE} o {@code DATE_IN_PAST}; 409 {@code DUPLICATE_SESSION}
     */
    @Transactional
    public SessionResponse create(AuthenticatedUser user, CreateSessionRequest request) {
        Group group = findActiveGroup(user, request.groupId());
        slotPolicy.requireValidSlot(request.date(), request.startTime(), request.endTime());
        groupRepository.findLockedById(group.getId());
        slotPolicy.requireFree(group.getId(), request.date(), request.startTime(), null);
        TrainingSession session = new TrainingSession(group, request.date(), request.startTime(), request.endTime());
        session.setNotes(TextNormalizer.trimToNull(request.notes()));
        return SessionMapper.toResponse(sessionRepository.save(session));
    }

    /**
     * A2: crea una sesión por cada franja del grupo y cada fecha del rango. Omite las que ya existen con la
     * misma fecha y hora de inicio.
     *
     * @param user    identidad autenticada
     * @param request grupo y rango
     * @return cuántas sesiones se crearon y omitieron, y las creadas
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED}; 422 {@code GROUP_INACTIVE},
     *                           {@code INVALID_DATE_RANGE}, {@code DATE_IN_PAST} o {@code GROUP_WITHOUT_SCHEDULES}
     */
    @Transactional
    public GenerateSessionsResponse generate(AuthenticatedUser user, GenerateSessionsRequest request) {
        Group group = findActiveGroup(user, request.groupId());
        DateRangePolicy.requireSessionRange(request.dateFrom(), request.dateTo());
        slotPolicy.requireNotPast(request.dateFrom());
        List<Schedule> schedules = scheduleRepository.findByGroupId(group.getId()).stream()
                .sorted(Comparator.comparing(Schedule::getStartTime))
                .toList();
        if (schedules.isEmpty()) {
            throw new BusinessException(ErrorCode.GROUP_WITHOUT_SCHEDULES, HttpStatus.UNPROCESSABLE_CONTENT,
                    WITHOUT_SCHEDULES_MESSAGE);
        }
        groupRepository.findLockedById(group.getId());
        Set<LocalDateTime> taken = sessionRepository
                .findByGroupIdAndSessionDateBetween(group.getId(), request.dateFrom(), request.dateTo()).stream()
                .map(session -> session.getSessionDate().atTime(session.getStartTime()))
                .collect(Collectors.toSet());
        List<TrainingSession> planned = plan(group, schedules, request.dateFrom(), request.dateTo());
        List<TrainingSession> created = planned.stream()
                .filter(session -> !taken.contains(session.getSessionDate().atTime(session.getStartTime())))
                .toList();
        List<SessionResponse> sessions = sessionRepository.saveAll(created).stream()
                .map(SessionMapper::toResponse)
                .toList();
        return new GenerateSessionsResponse(sessions.size(), planned.size() - sessions.size(), sessions);
    }

    /**
     * A5: cambia la fecha y las horas de una sesión programada. Las notas enviadas reemplazan las
     * anteriores.
     *
     * @param user      identidad autenticada
     * @param sessionId id de la sesión
     * @param request   nueva fecha, horas y notas
     * @return la sesión actualizada
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED}; 409 {@code INVALID_STATE} o
     *                           {@code DUPLICATE_SESSION}; 422 {@code INVALID_TIME_RANGE} o {@code DATE_IN_PAST}
     */
    @Transactional
    public SessionResponse reschedule(AuthenticatedUser user, Integer sessionId, UpdateSessionRequest request) {
        Integer groupId = sessionRepository.findGroupIdById(sessionId).orElseThrow(SessionService::sessionNotFound);
        groupRepository.findLockedById(groupId);
        TrainingSession session = findLockedSession(sessionId);
        accessPolicy.requireAccess(user, session.getGroup());
        requireScheduled(session);
        slotPolicy.requireValidSlot(request.date(), request.startTime(), request.endTime());
        slotPolicy.requireFree(groupId, request.date(), request.startTime(), sessionId);
        session.reschedule(request.date(), request.startTime(), request.endTime());
        session.setNotes(TextNormalizer.trimToNull(request.notes()));
        return SessionMapper.toResponse(session);
    }

    /**
     * A6: cancela una sesión programada y guarda el motivo en sus notas. Las sesiones canceladas no cuentan
     * en los informes de asistencia.
     *
     * @param user      identidad autenticada
     * @param sessionId id de la sesión
     * @param reason    motivo
     * @return la sesión cancelada
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED}; 409 {@code INVALID_STATE}
     */
    @Transactional
    public SessionResponse cancel(AuthenticatedUser user, Integer sessionId, String reason) {
        TrainingSession session = findLockedSession(sessionId);
        accessPolicy.requireAccess(user, session.getGroup());
        requireScheduled(session);
        session.setNotes(reason.trim());
        session.setStatus(SessionStatus.CANCELLED);
        return SessionMapper.toResponse(session);
    }

    /**
     * @return una sesión por franja y fecha del rango, en orden de fecha y hora, sin guardar
     */
    private static List<TrainingSession> plan(Group group, List<Schedule> schedules, LocalDate from, LocalDate to) {
        List<TrainingSession> planned = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            LocalDate day = date;
            schedules.stream()
                    .filter(schedule -> schedule.getDayOfWeek().equals(day.getDayOfWeek()))
                    .map(schedule -> new TrainingSession(group, day, schedule.getStartTime(), schedule.getEndTime()))
                    .forEach(planned::add);
        }
        return planned;
    }

    private Group findActiveGroup(AuthenticatedUser user, Integer groupId) {
        Group group = groupRepository.findDetailedById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        GROUP_NOT_FOUND_MESSAGE));
        accessPolicy.requireAccess(user, group);
        if (group.getStatus() != EntityStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.GROUP_INACTIVE, HttpStatus.UNPROCESSABLE_CONTENT,
                    GROUP_INACTIVE_MESSAGE);
        }
        return group;
    }

    private TrainingSession findLockedSession(Integer sessionId) {
        return sessionRepository.findLockedById(sessionId).orElseThrow(SessionService::sessionNotFound);
    }

    private static void requireScheduled(TrainingSession session) {
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.INVALID_STATE, HttpStatus.CONFLICT, NOT_SCHEDULED_MESSAGE);
        }
    }

    private static BusinessException sessionNotFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, SESSION_NOT_FOUND_MESSAGE);
    }
}
