package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.ChangePasswordRequest;
import com.uniquindio.teen_sport.dto.UpdateOwnProfileRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.UserProfileMapper;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyAccountServiceTest {

    private static final String PASSWORD = "Segura#2026";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final CoachRepository coachRepository = mock(CoachRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private MyAccountService service;

    @BeforeEach
    void setUp() {
        service = new MyAccountService(userRepository, new RoleProfileResolver(guardianRepository, coachRepository),
                new ProfileEditor(), new UserProfileMapper(), passwordEncoder, new PasswordPolicy());
    }

    @Test
    void guardianProfileIncludesGuardianDataOnly() {
        User user = account(1, RoleName.GUARDIAN);
        guardianOf(user, 10, "Calle 1");

        UserProfile profile = service.getProfile(1);

        assertThat(profile.id()).isEqualTo(1);
        assertThat(profile.role()).isEqualTo(RoleName.GUARDIAN);
        assertThat(profile.guardian()).isEqualTo(new UserProfile.GuardianData(10, "Calle 1"));
        assertThat(profile.coach()).isNull();
    }

    @Test
    void adminProfileHasNoRoleSpecificData() {
        account(2, RoleName.ADMIN);

        UserProfile profile = service.getProfile(2);

        assertThat(profile.guardian()).isNull();
        assertThat(profile.coach()).isNull();
    }

    @Test
    void missingAccountIsUnauthenticated() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        BusinessException ex = catchThrowableOfType(BusinessException.class, () -> service.getProfile(99));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void guardianUpdateAppliesOwnFieldsAndIgnoresCoachFields() {
        User user = account(1, RoleName.GUARDIAN);
        Guardian guardian = guardianOf(user, 10, "Calle 1");

        UserProfile profile = service.updateProfile(1,
                new UpdateOwnProfileRequest(" Ana ", " Gómez ", " 3110000000 ", " Carrera 5 ", "Fútbol", 9));

        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isEqualTo("Gómez");
        assertThat(user.getPhone()).isEqualTo("3110000000");
        assertThat(guardian.getAddress()).isEqualTo("Carrera 5");
        assertThat(profile.coach()).isNull();
    }

    @Test
    void omittedOptionalFieldOfTheRoleIsCleared() {
        User user = account(1, RoleName.GUARDIAN);
        Guardian guardian = guardianOf(user, 10, "Calle 1");

        service.updateProfile(1, new UpdateOwnProfileRequest("Ana", "Gómez", "300", null, null, null));

        assertThat(guardian.getAddress()).isNull();
    }

    @Test
    void coachUpdateAppliesCoachFieldsAndIgnoresAddress() {
        User user = account(3, RoleName.COACH);
        Coach coach = new Coach(user, "Natación", 2);
        ReflectionTestUtils.setField(coach, "id", 20);
        when(coachRepository.findByUserId(3)).thenReturn(Optional.of(coach));

        UserProfile profile = service.updateProfile(3,
                new UpdateOwnProfileRequest("Luis", "Pérez", "300", "Calle 9", " Fútbol ", 7));

        assertThat(coach.getSpecialty()).isEqualTo("Fútbol");
        assertThat(coach.getYearsExperience()).isEqualTo(7);
        assertThat(profile.coach()).isEqualTo(new UserProfile.CoachData(20, "Fútbol", 7));
        assertThat(profile.guardian()).isNull();
    }

    @Test
    void identityFieldsAreNeverChangedByProfileUpdate() {
        User user = account(2, RoleName.ADMIN);

        service.updateProfile(2, new UpdateOwnProfileRequest("Nuevo", "Nombre", "300", null, null, null));

        assertThat(user.getEmail()).isEqualTo("user2@teensport.test");
        assertThat(user.getDocumentNumber()).isEqualTo("1002");
        assertThat(user.getRole().getName()).isEqualTo(RoleName.ADMIN);
    }

    @Test
    void changesPasswordWhenCurrentPasswordIsCorrect() {
        User user = account(1, RoleName.GUARDIAN);

        service.changePassword(1, new ChangePasswordRequest(PASSWORD, "Nueva#Clave1"));

        assertThat(passwordEncoder.matches("Nueva#Clave1", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isFalse();
    }

    @Test
    void wrongCurrentPasswordIsRejectedAndKeepsThePassword() {
        User user = account(1, RoleName.GUARDIAN);
        String originalHash = user.getPasswordHash();

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.changePassword(1, new ChangePasswordRequest("Otra#Clave1", "Nueva#Clave1")));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INCORRECT_CURRENT_PASSWORD);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
    }

    @Test
    void currentPasswordAboveBcryptLimitIsIncorrect() {
        account(1, RoleName.GUARDIAN);

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.changePassword(1, new ChangePasswordRequest("Ñ".repeat(40), "Nueva#Clave1")));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.INCORRECT_CURRENT_PASSWORD);
    }

    @Test
    void weakNewPasswordIsRejectedAndKeepsThePassword() {
        User user = account(1, RoleName.GUARDIAN);
        String originalHash = user.getPasswordHash();

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.changePassword(1, new ChangePasswordRequest(PASSWORD, "sinmayuscula1#")));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.WEAK_PASSWORD);
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
    }

    private User account(int id, RoleName role) {
        User user = new User(new Role(role, null), "user" + id + "@teensport.test", passwordEncoder.encode(PASSWORD),
                Instant.parse("2026-10-01T00:00:00Z"));
        user.setDocumentType("CC");
        user.setDocumentNumber("100" + id);
        user.setFirstName("Nombre");
        user.setLastName("Apellido");
        user.setPhone("300");
        ReflectionTestUtils.setField(user, "id", id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private Guardian guardianOf(User user, int id, String address) {
        Guardian guardian = new Guardian(user, address);
        ReflectionTestUtils.setField(guardian, "id", id);
        when(guardianRepository.findByUserId(user.getId())).thenReturn(Optional.of(guardian));
        return guardian;
    }
}
