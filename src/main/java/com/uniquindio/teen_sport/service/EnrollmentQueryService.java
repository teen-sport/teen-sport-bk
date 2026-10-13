package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.dto.EnrollmentSummary;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.EnrollmentMapper;
import com.uniquindio.teen_sport.repository.EnrollmentFilter;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.EnrollmentSpecifications;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de matrículas: listar (E7) y consultar (E8). Un acudiente solo ve las matrículas de sus
 * deportistas ({@link AthleteAccessPolicy}).
 */
@Service
public class EnrollmentQueryService {

    static final String NOT_FOUND_MESSAGE = "La matrícula no existe.";

    /** Orden del listado: las más recientes primero e id para desempatar. */
    static final Sort ORDER = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final EnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final AthleteAccessPolicy accessPolicy;

    /**
     * @param enrollmentRepository matrículas
     * @param scheduleRepository   franjas
     * @param accessPolicy         regla de propiedad de los deportistas
     */
    public EnrollmentQueryService(EnrollmentRepository enrollmentRepository, ScheduleRepository scheduleRepository,
                                  AthleteAccessPolicy accessPolicy) {
        this.enrollmentRepository = enrollmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.accessPolicy = accessPolicy;
    }

    /**
     * E7: listado paginado. Para un acudiente el filtro {@code guardianId} se reemplaza por el suyo.
     *
     * @param user   identidad autenticada
     * @param filter filtros
     * @param page   índice de página, desde 0
     * @param size   tamaño de página
     * @return la página de matrículas, de la más reciente a la más antigua
     */
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentSummary> list(AuthenticatedUser user, EnrollmentFilter filter, int page, int size) {
        EnrollmentFilter scoped = filter.withGuardian(accessPolicy.guardianScope(user, filter.guardianId()));
        Page<Enrollment> enrollments = enrollmentRepository.findAll(EnrollmentSpecifications.enrollments(scoped),
                PageRequest.of(page, size, ORDER));
        return PageResponse.of(enrollments, enrollments.getContent().stream()
                .map(EnrollmentMapper::toSummary)
                .toList());
    }

    /**
     * E8: detalle de una matrícula con las franjas de su grupo.
     *
     * @param user         identidad autenticada
     * @param enrollmentId id de la matrícula
     * @return la matrícula
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED} si es de otro acudiente
     */
    @Transactional(readOnly = true)
    public EnrollmentDetail get(AuthenticatedUser user, Integer enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findDetailedById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
        accessPolicy.requireAccess(user, enrollment.getAthlete());
        return detail(enrollment);
    }

    /**
     * @param enrollment matrícula con su grupo cargado
     * @return el detalle con las franjas del grupo
     */
    EnrollmentDetail detail(Enrollment enrollment) {
        return EnrollmentMapper.toDetail(enrollment, scheduleRepository.findByGroupId(enrollment.getGroup().getId()));
    }
}
