package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.AttendanceRecordResponse;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.AttendanceRecord;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.mapper.AttendanceMapper;
import com.uniquindio.teen_sport.repository.AttendanceFilter;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import com.uniquindio.teen_sport.repository.AttendanceSpecifications;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.DateRangePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de asistencia (A9). El administrador ve todas las marcas; un entrenador, las de los grupos
 * que tiene a cargo; un acudiente, las de sus deportistas.
 */
@Service
public class AttendanceQueryService {

    /** Orden del historial: la sesión más reciente primero e id para desempatar. */
    static final Sort ORDER = Sort.by(Sort.Order.desc("session.sessionDate"), Sort.Order.desc("session.startTime"),
            Sort.Order.desc("id"));

    private final AttendanceRecordRepository attendanceRepository;
    private final GroupAccessPolicy groupAccessPolicy;
    private final AthleteAccessPolicy athleteAccessPolicy;

    /**
     * @param attendanceRepository marcas de asistencia
     * @param groupAccessPolicy    regla de propiedad de los grupos
     * @param athleteAccessPolicy  regla de propiedad de los deportistas
     */
    public AttendanceQueryService(AttendanceRecordRepository attendanceRepository,
                                  GroupAccessPolicy groupAccessPolicy, AthleteAccessPolicy athleteAccessPolicy) {
        this.attendanceRepository = attendanceRepository;
        this.groupAccessPolicy = groupAccessPolicy;
        this.athleteAccessPolicy = athleteAccessPolicy;
    }

    /**
     * @param user   identidad autenticada
     * @param filter filtros; su alcance ({@code coachId}, {@code guardianId}) se ignora y lo fija el rol
     * @param page   índice de página, desde 0
     * @param size   tamaño de página
     * @return la página de marcas
     * @throws BusinessException 422 {@code INVALID_DATE_RANGE} si {@code from} es posterior a {@code to}
     */
    @Transactional(readOnly = true)
    public PageResponse<AttendanceRecordResponse> list(AuthenticatedUser user, AttendanceFilter filter, int page,
                                                       int size) {
        DateRangePolicy.requireOrdered(filter.from(), filter.to());
        AttendanceFilter scoped = user.role() == RoleName.GUARDIAN
                ? filter.withScope(null, athleteAccessPolicy.ownGuardianId(user))
                : filter.withScope(groupAccessPolicy.coachScope(user, null), null);
        Page<AttendanceRecord> records = attendanceRepository.findAll(AttendanceSpecifications.records(scoped),
                PageRequest.of(page, size, ORDER));
        return PageResponse.of(records, records.getContent().stream()
                .map(AttendanceMapper::toResponse)
                .toList());
    }
}
