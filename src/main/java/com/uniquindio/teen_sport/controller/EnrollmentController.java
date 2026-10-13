package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.config.WebConfig;
import com.uniquindio.teen_sport.dto.CreateEnrollmentRequest;
import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.dto.EnrollmentSummary;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.PaymentStatus;
import com.uniquindio.teen_sport.repository.EnrollmentFilter;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.EnrollmentQueryService;
import com.uniquindio.teen_sport.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.YearMonth;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Matrículas (E6–E8), para administradores y acudientes. La propiedad de cada matrícula se comprueba
 * en el servicio con la identidad del token.
 */
@RestController
@RequestMapping("/enrollments")
@PreAuthorize("hasAnyRole('ADMIN', 'GUARDIAN')")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentQueryService queryService;
    private final CurrentUserService currentUserService;

    /**
     * @param enrollmentService  solicitud de matrículas
     * @param queryService       consultas de matrículas
     * @param currentUserService identidad del usuario autenticado
     */
    public EnrollmentController(EnrollmentService enrollmentService, EnrollmentQueryService queryService,
                                CurrentUserService currentUserService) {
        this.enrollmentService = enrollmentService;
        this.queryService = queryService;
        this.currentUserService = currentUserService;
    }

    /**
     * E6: solicita una matrícula.
     *
     * @param request deportista, grupo y mes de inicio
     * @return 201 con la matrícula y la cabecera {@code Location}
     */
    @PostMapping
    public ResponseEntity<EnrollmentDetail> createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        EnrollmentDetail enrollment = enrollmentService.create(currentUserService.current(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(WebConfig.API_PREFIX + "/enrollments/{id}")
                .buildAndExpand(enrollment.id())
                .toUri();
        return ResponseEntity.created(location).body(enrollment);
    }

    /**
     * E7: listado paginado; un acudiente solo recibe las de sus deportistas.
     *
     * @param page            índice de página, desde 0
     * @param size            tamaño de página, de 1 a 100
     * @param status          filtro por estado
     * @param paymentStatus   filtro por estado de pago
     * @param paymentReported filtro por pago reportado
     * @param period          matrículas vigentes en ese mes {@code YYYY-MM}
     * @param disciplineId    filtro por disciplina
     * @param groupId         filtro por grupo
     * @param athleteId       filtro por deportista
     * @param guardianId      filtro por acudiente (solo administrador)
     * @return 200 con la página
     */
    @GetMapping
    public PageResponse<EnrollmentSummary> listEnrollments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) Boolean paymentReported,
            @RequestParam(required = false) YearMonth period,
            @RequestParam(required = false) Integer disciplineId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) Integer guardianId) {
        EnrollmentFilter filter = new EnrollmentFilter(status, paymentStatus, paymentReported, period, disciplineId,
                groupId, athleteId, guardianId);
        return queryService.list(currentUserService.current(), filter, page, size);
    }

    /**
     * E8: detalle de una matrícula con las franjas del grupo.
     *
     * @param id id de la matrícula
     * @return 200 con la matrícula
     */
    @GetMapping("/{id}")
    public EnrollmentDetail getEnrollment(@PathVariable Integer id) {
        return queryService.get(currentUserService.current(), id);
    }
}
