package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.AthleteResponse;
import com.uniquindio.teen_sport.dto.CreateAthleteRequest;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.UpdateAthleteRequest;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.FieldValidationException;
import com.uniquindio.teen_sport.repository.AthleteRepository;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AthleteServiceTest {

    /** 12 de octubre de 2026 a mediodía en Bogotá. */
    private static final Instant NOW = Instant.parse("2026-10-12T17:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate FOURTEEN = TODAY.minusYears(14);

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, RoleName.ADMIN, null, null);
    private static final AuthenticatedUser GUARDIAN = new AuthenticatedUser(2, RoleName.GUARDIAN, 20, null);
    private static final AuthenticatedUser OTHER_GUARDIAN = new AuthenticatedUser(3, RoleName.GUARDIAN, 30, null);

    private final AthleteRepository athleteRepository = mock(AthleteRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private AthleteService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new AthleteService(athleteRepository, guardianRepository, enrollmentRepository, new AthleteAccessPolicy(),
                new AcademyCalendar(clock, "America/Bogota"), clock);
        when(athleteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void guardianRegistersAthleteForThemselvesIgnoringTheRequestedGuardian() {
        Guardian own = guardian(20, true, EntityStatus.ACTIVE);

        AthleteResponse athlete = service.create(GUARDIAN, createRequest(99, " 1001 ", FOURTEEN));

        ArgumentCaptor<Athlete> saved = ArgumentCaptor.forClass(Athlete.class);
        verify(athleteRepository).save(saved.capture());
        assertThat(saved.getValue().getGuardian()).isSameAs(own);
        assertThat(saved.getValue().getDocumentNumber()).isEqualTo("1001");
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(athlete.guardianId()).isEqualTo(20);
        assertThat(athlete.age()).isEqualTo(14);
        assertThat(athlete.hasActiveEnrollment()).isFalse();
        verify(guardianRepository, never()).findWithUserById(99);
    }

    @Test
    void adminMustIndicateAnExistingActiveGuardian() {
        guardian(40, true, EntityStatus.INACTIVE);

        FieldValidationException missing = catchThrowableOfType(FieldValidationException.class,
                () -> service.create(ADMIN, createRequest(null, "1001", FOURTEEN)));
        FieldValidationException unknown = catchThrowableOfType(FieldValidationException.class,
                () -> service.create(ADMIN, createRequest(77, "1001", FOURTEEN)));
        FieldValidationException inactive = catchThrowableOfType(FieldValidationException.class,
                () -> service.create(ADMIN, createRequest(40, "1001", FOURTEEN)));

        assertThat(missing.getField()).isEqualTo("guardianId");
        assertThat(unknown.getField()).isEqualTo("guardianId");
        assertThat(inactive.getField()).isEqualTo("guardianId");
        verify(athleteRepository, never()).save(any());
    }

    @Test
    void adminRegistersAthleteForAnActiveGuardian() {
        guardian(40, true, EntityStatus.ACTIVE);

        assertThat(service.create(ADMIN, createRequest(40, "1001", FOURTEEN)).guardianId()).isEqualTo(40);
    }

    @Test
    void guardianWithoutDataPolicyAcceptanceCannotRegisterAthletes() {
        guardian(20, false, EntityStatus.ACTIVE);

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.create(GUARDIAN, createRequest(null, "1001", FOURTEEN)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_POLICY_NOT_ACCEPTED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void registrationRejectsAgeOutOfRangeAndDuplicateDocument() {
        guardian(20, true, EntityStatus.ACTIVE);
        when(athleteRepository.existsByDocumentNumber("1001")).thenReturn(true);

        assertThat(failure(() -> service.create(GUARDIAN, createRequest(null, "2002", TODAY.minusYears(18))))
                .getCode()).isEqualTo(ErrorCode.AGE_OUT_OF_RANGE);
        BusinessException duplicate = failure(() -> service.create(GUARDIAN, createRequest(null, "1001", FOURTEEN)));
        assertThat(duplicate.getCode()).isEqualTo(ErrorCode.DUPLICATE_ATHLETE);
        assertThat(duplicate.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void ageUsesTheAcademyDateNotTheUtcDate() {
        // 9 p. m. del 11 de octubre en Bogotá: en UTC ya es 12 de octubre, el cumpleaños 12.
        Clock evening = Clock.fixed(Instant.parse("2026-10-12T02:00:00Z"), ZoneOffset.UTC);
        AthleteService atNight = new AthleteService(athleteRepository, guardianRepository, enrollmentRepository, new AthleteAccessPolicy(),
                new AcademyCalendar(evening, "America/Bogota"), evening);
        guardian(20, true, EntityStatus.ACTIVE);

        BusinessException ex = failure(() -> atNight.create(GUARDIAN,
                createRequest(null, "1001", LocalDate.of(2014, 10, 12))));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.AGE_OUT_OF_RANGE);
    }

    @Test
    void guardianCannotSeeOrEditAnotherGuardiansAthlete() {
        existingAthlete(5, guardian(20, true, EntityStatus.ACTIVE), FOURTEEN);

        BusinessException read = failure(() -> service.get(OTHER_GUARDIAN, 5));
        BusinessException write = failure(() -> service.update(OTHER_GUARDIAN, 5, updateRequest("1001", FOURTEEN)));

        assertThat(read.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(read.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(write.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(service.get(GUARDIAN, 5).id()).isEqualTo(5);
        assertThat(service.get(ADMIN, 5).id()).isEqualTo(5);
    }

    @Test
    void missingAthleteIsNotFound() {
        when(athleteRepository.findById(9)).thenReturn(Optional.empty());

        assertThat(failure(() -> service.get(ADMIN, 9)).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void athleteWhoTurnedEighteenCanStillBeCorrectedIfBirthDateDoesNotChange() {
        LocalDate eighteen = TODAY.minusYears(18).minusDays(3);
        Athlete athlete = existingAthlete(5, guardian(20, true, EntityStatus.ACTIVE), eighteen);

        AthleteResponse updated = service.update(GUARDIAN, 5, new UpdateAthleteRequest("Corregido", "Apellido", "TI",
                "1001", eighteen, null));

        assertThat(athlete.getFirstName()).isEqualTo("Corregido");
        assertThat(updated.age()).isEqualTo(18);
    }

    @Test
    void changingBirthDateOutOfRangeIsRejected() {
        existingAthlete(5, guardian(20, true, EntityStatus.ACTIVE), FOURTEEN);

        assertThat(failure(() -> service.update(GUARDIAN, 5, updateRequest("1001", TODAY.minusYears(10)))).getCode())
                .isEqualTo(ErrorCode.AGE_OUT_OF_RANGE);
    }

    @Test
    void updateRejectsDocumentOfAnotherAthlete() {
        existingAthlete(5, guardian(20, true, EntityStatus.ACTIVE), FOURTEEN);
        when(athleteRepository.existsByDocumentNumberAndIdNot("2002", 5)).thenReturn(true);

        assertThat(failure(() -> service.update(GUARDIAN, 5, updateRequest("2002", FOURTEEN))).getCode())
                .isEqualTo(ErrorCode.DUPLICATE_ATHLETE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listComputesAgeAndKeepsPageMetadata() {
        Athlete athlete = existingAthlete(5, guardian(20, true, EntityStatus.ACTIVE), FOURTEEN);
        Pageable pageable = PageRequest.of(0, 10);
        when(athleteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(athlete), pageable, 1));

        PageResponse<AthleteResponse> page = service.list(GUARDIAN, 99, "ana", 0, 10);

        assertThat(page.content()).singleElement().satisfies(item -> assertThat(item.age()).isEqualTo(14));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listFlagsAthletesWithEnrollmentsInForceInOneQuery() {
        Guardian guardian = guardian(20, true, EntityStatus.ACTIVE);
        Athlete enrolled = existingAthlete(5, guardian, FOURTEEN);
        Athlete free = existingAthlete(6, guardian, FOURTEEN);
        when(athleteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(enrolled, free), PageRequest.of(0, 10), 2));
        when(enrollmentRepository.findAthleteIdsWithStatus(List.of(5, 6), EnrollmentStatus.IN_FORCE))
                .thenReturn(List.of(5));

        PageResponse<AthleteResponse> page = service.list(ADMIN, null, null, 0, 10);

        assertThat(page.content()).extracting(AthleteResponse::hasActiveEnrollment).containsExactly(true, false);
        verify(enrollmentRepository, never()).existsByAthleteIdAndStatusIn(any(), any());
    }

    @Test
    void detailReportsEnrollmentInForce() {
        existingAthlete(5, guardian(20, true, EntityStatus.ACTIVE), FOURTEEN);
        when(enrollmentRepository.existsByAthleteIdAndStatusIn(5, EnrollmentStatus.IN_FORCE)).thenReturn(true);

        assertThat(service.get(GUARDIAN, 5).hasActiveEnrollment()).isTrue();
    }

    private Guardian guardian(int id, boolean acceptedPolicy, EntityStatus status) {
        User user = new User(new Role(RoleName.GUARDIAN, null), "g" + id + "@teensport.test", "$2a$hash", NOW);
        if (acceptedPolicy) {
            user.acceptDataPolicy(NOW);
        }
        user.setStatus(status);
        Guardian guardian = new Guardian(user, null);
        ReflectionTestUtils.setField(guardian, "id", id);
        when(guardianRepository.findWithUserById(id)).thenReturn(Optional.of(guardian));
        return guardian;
    }

    private Athlete existingAthlete(int id, Guardian guardian, LocalDate birthDate) {
        Athlete athlete = new Athlete(guardian, NOW);
        athlete.setFirstName("Ana");
        athlete.setLastName("Prueba");
        athlete.setDocumentType("TI");
        athlete.setDocumentNumber("1001");
        athlete.setBirthDate(birthDate);
        ReflectionTestUtils.setField(athlete, "id", id);
        when(athleteRepository.findById(id)).thenReturn(Optional.of(athlete));
        return athlete;
    }

    private static CreateAthleteRequest createRequest(Integer guardianId, String idNumber, LocalDate birthDate) {
        return new CreateAthleteRequest(guardianId, "Ana", "Prueba", "TI", idNumber, birthDate, " FEMALE ");
    }

    private static UpdateAthleteRequest updateRequest(String idNumber, LocalDate birthDate) {
        return new UpdateAthleteRequest("Ana", "Prueba", "TI", idNumber, birthDate, null);
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
