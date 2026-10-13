package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.AthleteData;
import com.uniquindio.teen_sport.dto.AthleteResponse;
import com.uniquindio.teen_sport.dto.CreateAthleteRequest;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.UpdateAthleteRequest;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.FieldValidationException;
import com.uniquindio.teen_sport.repository.AthleteRepository;
import com.uniquindio.teen_sport.repository.AthleteSpecifications;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.validation.AthleteAgePolicy;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de deportistas: registrar (E1), listar (E2), consultar (E3) y editar (E4).
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>Propiedad: un acudiente solo ve y edita sus deportistas ({@link AthleteAccessPolicy}).</li>
 *   <li>Edad: de 12 a 17 años en la fecha de la solicitud, según la zona de la academia. Al editar
 *       solo se valida si cambia la fecha de nacimiento, para poder corregir los datos de un
 *       deportista que ya cumplió 18.</li>
 *   <li>El documento es único entre deportistas ({@code DUPLICATE_ATHLETE}).</li>
 *   <li>El acudiente debe haber aceptado la política de datos y, si lo indica un administrador,
 *       existir y estar activo.</li>
 *   <li>{@code hasActiveEnrollment} indica si el deportista tiene alguna matrícula vigente
 *       ({@code PENDING_REVIEW} o {@code ACTIVE}).</li>
 * </ul>
 */
@Service
public class AthleteService {

    static final String NOT_FOUND_MESSAGE = "El deportista no existe.";
    static final String DUPLICATE_MESSAGE = "Ya existe un deportista con ese número de documento.";
    static final String DATA_POLICY_MESSAGE =
            "El acudiente debe aceptar la política de tratamiento de datos personales.";
    static final String GUARDIAN_REQUIRED_MESSAGE = "Debes indicar el acudiente del deportista.";
    static final String GUARDIAN_INVALID_MESSAGE = "El acudiente no existe o está inactivo.";
    static final String GUARDIAN_ID_FIELD = "guardianId";

    /** Orden del listado: apellidos, nombres e id para desempatar. */
    static final Sort ORDER = Sort.by("lastName", "firstName", "id");

    private final AthleteRepository athleteRepository;
    private final GuardianRepository guardianRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AthleteAccessPolicy accessPolicy;
    private final AcademyCalendar calendar;
    private final Clock clock;

    /**
     * @param athleteRepository  deportistas
     * @param guardianRepository   acudientes
     * @param enrollmentRepository matrículas
     * @param accessPolicy         regla de propiedad
     * @param calendar             fecha de negocio de la academia
     * @param clock                reloj de la aplicación
     */
    public AthleteService(AthleteRepository athleteRepository, GuardianRepository guardianRepository,
                          EnrollmentRepository enrollmentRepository, AthleteAccessPolicy accessPolicy,
                          AcademyCalendar calendar, Clock clock) {
        this.athleteRepository = athleteRepository;
        this.guardianRepository = guardianRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.accessPolicy = accessPolicy;
        this.calendar = calendar;
        this.clock = clock;
    }

    /**
     * E1: registra un deportista para el acudiente del token o, si es un administrador, para el
     * acudiente indicado.
     *
     * @param user    identidad autenticada
     * @param request datos del deportista
     * @return el deportista creado
     * @throws FieldValidationException 400 en {@code guardianId} si el administrador no indica un acudiente válido
     * @throws BusinessException        422 {@code AGE_OUT_OF_RANGE} o {@code DATA_POLICY_NOT_ACCEPTED};
     *                                  409 {@code DUPLICATE_ATHLETE}
     */
    @Transactional
    public AthleteResponse create(AuthenticatedUser user, CreateAthleteRequest request) {
        Guardian guardian = responsibleGuardian(user, request.guardianId());
        LocalDate today = calendar.today();
        AthleteAgePolicy.requireEligible(request.birthDate(), today);
        if (!guardian.getUser().isAcceptedDataPolicy()) {
            throw new BusinessException(ErrorCode.DATA_POLICY_NOT_ACCEPTED, HttpStatus.UNPROCESSABLE_CONTENT,
                    DATA_POLICY_MESSAGE);
        }
        String idNumber = request.idNumber().trim();
        if (athleteRepository.existsByDocumentNumber(idNumber)) {
            throw duplicate();
        }
        Athlete athlete = new Athlete(guardian, clock.instant());
        apply(athlete, request);
        return toResponse(athleteRepository.save(athlete), today, false);
    }

    /**
     * E2: listado paginado. Un acudiente solo recibe sus deportistas; el filtro {@code guardianId}
     * solo lo usa un administrador. Las matrículas vigentes de la página se consultan en una sola
     * consulta.
     *
     * @param user       identidad autenticada
     * @param guardianId filtro por acudiente (administrador)
     * @param text       búsqueda libre sobre nombres y documento
     * @param page       índice de página, desde 0
     * @param size       tamaño de página
     * @return la página de deportistas
     */
    @Transactional(readOnly = true)
    public PageResponse<AthleteResponse> list(AuthenticatedUser user, Integer guardianId, String text,
                                              int page, int size) {
        Integer scope = accessPolicy.guardianScope(user, guardianId);
        Page<Athlete> athletes = athleteRepository.findAll(AthleteSpecifications.athletes(scope, text),
                PageRequest.of(page, size, ORDER));
        LocalDate today = calendar.today();
        Set<Integer> enrolled = athletes.isEmpty() ? Set.of() : new HashSet<>(enrollmentRepository
                .findAthleteIdsWithStatus(athletes.map(Athlete::getId).getContent(), EnrollmentStatus.IN_FORCE));
        return PageResponse.of(athletes, athletes.getContent().stream()
                .map(athlete -> toResponse(athlete, today, enrolled.contains(athlete.getId())))
                .toList());
    }

    /**
     * E3: detalle de un deportista.
     *
     * @param user      identidad autenticada
     * @param athleteId id del deportista
     * @return el deportista
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED} si es de otro acudiente
     */
    @Transactional(readOnly = true)
    public AthleteResponse get(AuthenticatedUser user, Integer athleteId) {
        return toResponse(findAccessible(user, athleteId), calendar.today());
    }

    /**
     * E4: edita los datos de un deportista. El acudiente responsable no cambia.
     *
     * @param user      identidad autenticada
     * @param athleteId id del deportista
     * @param request   nuevos datos
     * @return el deportista actualizado
     * @throws BusinessException 404 {@code NOT_FOUND}; 403 {@code ACCESS_DENIED};
     *                           422 {@code AGE_OUT_OF_RANGE}; 409 {@code DUPLICATE_ATHLETE}
     */
    @Transactional
    public AthleteResponse update(AuthenticatedUser user, Integer athleteId, UpdateAthleteRequest request) {
        Athlete athlete = findAccessible(user, athleteId);
        LocalDate today = calendar.today();
        if (!request.birthDate().equals(athlete.getBirthDate())) {
            AthleteAgePolicy.requireEligible(request.birthDate(), today);
        }
        if (athleteRepository.existsByDocumentNumberAndIdNot(request.idNumber().trim(), athleteId)) {
            throw duplicate();
        }
        apply(athlete, request);
        return toResponse(athlete, today);
    }

    private Guardian responsibleGuardian(AuthenticatedUser user, Integer requestedGuardianId) {
        if (user.role() != RoleName.ADMIN) {
            return guardianRepository.findWithUserById(accessPolicy.ownGuardianId(user))
                    .orElseThrow(() -> new IllegalStateException("El token referencia un acudiente inexistente"));
        }
        if (requestedGuardianId == null) {
            throw new FieldValidationException(GUARDIAN_ID_FIELD, GUARDIAN_REQUIRED_MESSAGE);
        }
        return guardianRepository.findWithUserById(requestedGuardianId)
                .filter(guardian -> guardian.getUser().isActive())
                .orElseThrow(() -> new FieldValidationException(GUARDIAN_ID_FIELD, GUARDIAN_INVALID_MESSAGE));
    }

    private Athlete findAccessible(AuthenticatedUser user, Integer athleteId) {
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
        accessPolicy.requireAccess(user, athlete);
        return athlete;
    }

    private static void apply(Athlete athlete, AthleteData data) {
        athlete.setFirstName(data.firstNames().trim());
        athlete.setLastName(data.lastNames().trim());
        athlete.setDocumentType(data.idType().trim());
        athlete.setDocumentNumber(data.idNumber().trim());
        athlete.setBirthDate(data.birthDate());
        athlete.setGender(TextNormalizer.trimToNull(data.gender()));
    }

    private AthleteResponse toResponse(Athlete athlete, LocalDate today) {
        return toResponse(athlete, today,
                enrollmentRepository.existsByAthleteIdAndStatusIn(athlete.getId(), EnrollmentStatus.IN_FORCE));
    }

    private static AthleteResponse toResponse(Athlete athlete, LocalDate today, boolean hasActiveEnrollment) {
        return new AthleteResponse(athlete.getId(), athlete.getGuardian().getId(), athlete.getFirstName(),
                athlete.getLastName(), athlete.getDocumentType(), athlete.getDocumentNumber(),
                athlete.getBirthDate(), AthleteAgePolicy.ageOn(athlete.getBirthDate(), today), athlete.getGender(),
                hasActiveEnrollment);
    }

    private static BusinessException duplicate() {
        return new BusinessException(ErrorCode.DUPLICATE_ATHLETE, HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
    }
}
