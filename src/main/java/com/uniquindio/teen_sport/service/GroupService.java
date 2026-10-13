package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CreateGroupRequest;
import com.uniquindio.teen_sport.dto.GroupResponse;
import com.uniquindio.teen_sport.dto.UpdateGroupRequest;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.GroupMapper;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.DisciplineRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.GroupSpecifications;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de grupos: listar (H6), consultar (H7), crear (H8), editar (H9), activar o
 * desactivar (H10) y asignar entrenador (H11).
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>La capacidad máxima va de {@value #MIN_CAPACITY} a {@value #MAX_CAPACITY} ({@code INVALID_MAX_CAPACITY}).</li>
 *   <li>Al crear, la disciplina y el entrenador deben estar activos.</li>
 *   <li>Un entrenador solo consulta sus grupos ({@link GroupAccessPolicy}).</li>
 *   <li>El nuevo entrenador de un grupo debe estar activo y libre en todas sus franjas.</li>
 *   <li>Al reactivar un grupo, su entrenador debe estar libre en todas las franjas del grupo.</li>
 * </ul>
 *
 * <p>Pendientes de fases posteriores: {@code CAPACITY_BELOW_OCCUPIED} (H9) y {@code GROUP_HAS_ACTIVITY}
 * (H10) necesitan las matrículas y las sesiones, y en H11 las sesiones futuras pasarán al nuevo
 * entrenador cuando existan.</p>
 */
@Service
public class GroupService {

    /** Capacidad mínima de un grupo. */
    public static final int MIN_CAPACITY = 1;

    /** Capacidad máxima de un grupo. */
    public static final int MAX_CAPACITY = 15;

    static final String GROUP_NOT_FOUND_MESSAGE = "El grupo no existe.";
    static final String DISCIPLINE_NOT_FOUND_MESSAGE = "La disciplina no existe.";
    static final String COACH_NOT_FOUND_MESSAGE = "El entrenador no existe.";
    static final String INVALID_CAPACITY_MESSAGE = "La capacidad máxima debe estar entre 1 y 15 deportistas.";
    static final String INACTIVE_COACH_MESSAGE = "El entrenador está inactivo.";
    static final String INACTIVE_DISCIPLINE_MESSAGE = "La disciplina está inactiva.";

    /** Orden del listado: nombre y, para desempatar, id. */
    static final Sort ORDER = Sort.by("name", "id");

    private final GroupRepository groupRepository;
    private final ScheduleRepository scheduleRepository;
    private final DisciplineRepository disciplineRepository;
    private final CoachRepository coachRepository;
    private final GroupAccessPolicy accessPolicy;
    private final ScheduleConflictChecker conflictChecker;
    private final GroupMapper groupMapper;

    /**
     * @param groupRepository      grupos
     * @param scheduleRepository   franjas
     * @param disciplineRepository disciplinas
     * @param coachRepository      entrenadores
     * @param accessPolicy         regla de propiedad para entrenadores
     * @param conflictChecker      detección de cruces de franjas
     * @param groupMapper          salida en el formato del contrato
     */
    public GroupService(GroupRepository groupRepository, ScheduleRepository scheduleRepository,
                        DisciplineRepository disciplineRepository, CoachRepository coachRepository,
                        GroupAccessPolicy accessPolicy, ScheduleConflictChecker conflictChecker,
                        GroupMapper groupMapper) {
        this.groupRepository = groupRepository;
        this.scheduleRepository = scheduleRepository;
        this.disciplineRepository = disciplineRepository;
        this.coachRepository = coachRepository;
        this.accessPolicy = accessPolicy;
        this.conflictChecker = conflictChecker;
        this.groupMapper = groupMapper;
    }

    /**
     * H6: grupos con sus franjas. Un entrenador solo recibe los suyos; el filtro {@code coachId} es del
     * administrador. Las franjas de todos los grupos se cargan en una sola consulta.
     *
     * @param user         identidad autenticada
     * @param disciplineId filtro por disciplina
     * @param coachId      filtro por entrenador (administrador)
     * @param status       filtro por estado
     * @return los grupos ordenados por nombre
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> list(AuthenticatedUser user, Integer disciplineId, Integer coachId,
                                    EntityStatus status) {
        Integer scope = accessPolicy.coachScope(user, coachId);
        List<Group> groups = groupRepository.findAll(GroupSpecifications.groups(disciplineId, scope, status), ORDER);
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Schedule>> schedules = scheduleRepository
                .findByGroupIds(groups.stream().map(Group::getId).toList()).stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getGroup().getId()));
        return groups.stream()
                .map(group -> groupMapper.toResponse(group, schedules.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    /**
     * H7: detalle de un grupo con sus franjas.
     *
     * @param user    identidad autenticada
     * @param groupId id del grupo
     * @return el grupo
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED} si es de otro entrenador
     */
    @Transactional(readOnly = true)
    public GroupResponse get(AuthenticatedUser user, Integer groupId) {
        Group group = findDetailed(groupId);
        accessPolicy.requireAccess(user, group);
        return toResponse(group);
    }

    /**
     * H8: crea un grupo activo y sin franjas.
     *
     * @param request datos del grupo
     * @return el grupo creado
     * @throws BusinessException 422 {@code INVALID_MAX_CAPACITY}, {@code DISCIPLINE_INACTIVE} o
     *                           {@code INVALID_COACH}; 404 {@code NOT_FOUND} si la disciplina o el entrenador no existen
     */
    @Transactional
    public GroupResponse create(CreateGroupRequest request) {
        requireValidCapacity(request.maxCapacity());
        Discipline discipline = disciplineRepository.findById(request.disciplineId())
                .orElseThrow(() -> notFound(DISCIPLINE_NOT_FOUND_MESSAGE));
        if (discipline.getStatus() != EntityStatus.ACTIVE) {
            throw unprocessable(ErrorCode.DISCIPLINE_INACTIVE, INACTIVE_DISCIPLINE_MESSAGE);
        }
        Coach coach = findActiveCoach(request.coachId());
        Group group = groupRepository.save(new Group(discipline, coach, request.name().trim(), request.maxCapacity()));
        return groupMapper.toResponse(group, List.of());
    }

    /**
     * H9: edita el nombre y la capacidad.
     *
     * @param groupId id del grupo
     * @param request nuevos datos
     * @return el grupo actualizado
     * @throws BusinessException 404 {@code NOT_FOUND}; 422 {@code INVALID_MAX_CAPACITY}
     */
    @Transactional
    public GroupResponse update(Integer groupId, UpdateGroupRequest request) {
        Group group = findDetailed(groupId);
        requireValidCapacity(request.maxCapacity());
        group.setName(request.name().trim());
        group.setMaxCapacity(request.maxCapacity());
        return toResponse(group);
    }

    /**
     * H10: activa o desactiva un grupo.
     *
     * <p>Como las franjas de los grupos inactivos no bloquean al entrenador, al reactivar un grupo se
     * comprueba que sus franjas no se crucen con las de los demás grupos activos del mismo
     * entrenador. Se bloquean el grupo y el entrenador, igual que al asignar franjas o entrenador.</p>
     *
     * @param groupId id del grupo
     * @param status  nuevo estado
     * @return el grupo actualizado
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code COACH_SCHEDULE_CONFLICT} al reactivar
     */
    @Transactional
    public GroupResponse changeStatus(Integer groupId, EntityStatus status) {
        Group group = findDetailed(groupId);
        if (status == EntityStatus.ACTIVE && group.getStatus() != EntityStatus.ACTIVE) {
            groupRepository.findLockedById(groupId);
            coachRepository.findLockedById(group.getCoach().getId());
            conflictChecker.requireCoachAvailable(group, group.getCoach().getId());
        }
        group.setStatus(status);
        return toResponse(group);
    }

    /**
     * H11: asigna o cambia el entrenador del grupo. Bloquea el grupo y el nuevo entrenador para que
     * ningún cambio simultáneo de franjas invalide la comprobación de cruces.
     *
     * @param groupId id del grupo
     * @param coachId id del nuevo entrenador
     * @return el grupo actualizado
     * @throws BusinessException 404 {@code NOT_FOUND}; 422 {@code INVALID_COACH};
     *                           409 {@code COACH_SCHEDULE_CONFLICT}
     */
    @Transactional
    public GroupResponse assignCoach(Integer groupId, Integer coachId) {
        Group group = groupRepository.findLockedById(groupId).orElseThrow(() -> notFound(GROUP_NOT_FOUND_MESSAGE));
        Coach coach = findActiveCoach(coachId);
        coachRepository.findLockedById(coachId);
        if (!coach.getId().equals(group.getCoach().getId())) {
            conflictChecker.requireCoachAvailable(group, coach.getId());
            group.setCoach(coach);
        }
        return toResponse(group);
    }

    private Coach findActiveCoach(Integer coachId) {
        Coach coach = coachRepository.findWithUserById(coachId).orElseThrow(() -> notFound(COACH_NOT_FOUND_MESSAGE));
        if (!coach.getUser().isActive()) {
            throw unprocessable(ErrorCode.INVALID_COACH, INACTIVE_COACH_MESSAGE);
        }
        return coach;
    }

    private Group findDetailed(Integer groupId) {
        return groupRepository.findDetailedById(groupId).orElseThrow(() -> notFound(GROUP_NOT_FOUND_MESSAGE));
    }

    private GroupResponse toResponse(Group group) {
        return groupMapper.toResponse(group, scheduleRepository.findByGroupId(group.getId()));
    }

    private static void requireValidCapacity(Integer maxCapacity) {
        if (maxCapacity < MIN_CAPACITY || maxCapacity > MAX_CAPACITY) {
            throw unprocessable(ErrorCode.INVALID_MAX_CAPACITY, INVALID_CAPACITY_MESSAGE);
        }
    }

    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    private static BusinessException unprocessable(ErrorCode code, String message) {
        return new BusinessException(code, HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
