package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.LoginRequest;
import com.uniquindio.teen_sport.dto.LoginResponse;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.security.JwtTokenService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String PASSWORD = "Segura#2026";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final CoachRepository coachRepository = mock(CoachRepository.class);
    private final JwtTokenService tokenService = mock(JwtTokenService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, guardianRepository, coachRepository, passwordEncoder, tokenService);
        when(tokenService.issue(any())).thenReturn("signed-token");
        when(tokenService.expirationSeconds()).thenReturn(3600L);
    }

    @Test
    void guardianLoginReturnsTokenAndGuardianIdWithNormalizedEmail() {
        User user = user(10, RoleName.GUARDIAN, "ana@teensport.test");
        Guardian guardian = new Guardian(user, null);
        ReflectionTestUtils.setField(guardian, "id", 4);
        when(userRepository.findByEmail("ana@teensport.test")).thenReturn(Optional.of(user));
        when(guardianRepository.findByUserId(10)).thenReturn(Optional.of(guardian));

        LoginResponse response = service.login(new LoginRequest("  Ana@TeenSport.TEST ", PASSWORD));

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
        assertThat(response.user().id()).isEqualTo(10);
        assertThat(response.user().role()).isEqualTo(RoleName.GUARDIAN);
        assertThat(response.user().guardianId()).isEqualTo(4);
        assertThat(response.user().coachId()).isNull();
        verify(tokenService).issue(new AuthenticatedUser(10, RoleName.GUARDIAN, 4, null));
    }

    @Test
    void coachLoginCarriesCoachId() {
        User user = user(11, RoleName.COACH, "coach@teensport.test");
        Coach coach = new Coach(user, "Fútbol", 5);
        ReflectionTestUtils.setField(coach, "id", 6);
        when(userRepository.findByEmail("coach@teensport.test")).thenReturn(Optional.of(user));
        when(coachRepository.findByUserId(11)).thenReturn(Optional.of(coach));

        LoginResponse response = service.login(new LoginRequest("coach@teensport.test", PASSWORD));

        assertThat(response.user().coachId()).isEqualTo(6);
        assertThat(response.user().guardianId()).isNull();
    }

    @Test
    void adminLoginHasNoProfileIds() {
        when(userRepository.findByEmail("admin@teensport.test"))
                .thenReturn(Optional.of(user(1, RoleName.ADMIN, "admin@teensport.test")));

        LoginResponse response = service.login(new LoginRequest("admin@teensport.test", PASSWORD));

        assertThat(response.user().guardianId()).isNull();
        assertThat(response.user().coachId()).isNull();
    }

    @Test
    void wrongPasswordAndUnknownEmailFailWithTheSameError() {
        when(userRepository.findByEmail("ana@teensport.test"))
                .thenReturn(Optional.of(user(10, RoleName.GUARDIAN, "ana@teensport.test")));
        when(userRepository.findByEmail("nobody@teensport.test")).thenReturn(Optional.empty());

        BusinessException wrongPassword = loginFailure("ana@teensport.test", "Otra#2026");
        BusinessException unknownEmail = loginFailure("nobody@teensport.test", PASSWORD);

        for (BusinessException ex : new BusinessException[]{wrongPassword, unknownEmail}) {
            assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(wrongPassword.getMessage()).isEqualTo(unknownEmail.getMessage());
        verify(tokenService, never()).issue(any());
    }

    @Test
    void passwordAboveBcryptLimitIsRejectedAsInvalidCredentials() {
        when(userRepository.findByEmail("ana@teensport.test"))
                .thenReturn(Optional.of(user(10, RoleName.GUARDIAN, "ana@teensport.test")));

        assertThat(loginFailure("ana@teensport.test", "Ñ".repeat(40)).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void inactiveUserWithCorrectPasswordIsForbidden() {
        User user = user(10, RoleName.GUARDIAN, "ana@teensport.test");
        user.setStatus(EntityStatus.INACTIVE);
        when(userRepository.findByEmail("ana@teensport.test")).thenReturn(Optional.of(user));

        BusinessException ex = loginFailure("ana@teensport.test", PASSWORD);

        assertThat(ex.getCode()).isEqualTo(ErrorCode.USER_INACTIVE);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void inactiveUserWithWrongPasswordGetsInvalidCredentialsNotItsStatus() {
        User user = user(10, RoleName.GUARDIAN, "ana@teensport.test");
        user.setStatus(EntityStatus.INACTIVE);
        when(userRepository.findByEmail("ana@teensport.test")).thenReturn(Optional.of(user));

        assertThat(loginFailure("ana@teensport.test", "Otra#2026").getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private BusinessException loginFailure(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        return catchThrowableOfType(BusinessException.class, () -> service.login(request));
    }

    private User user(int id, RoleName roleName, String email) {
        User user = new User(new Role(roleName, null), email, passwordEncoder.encode(PASSWORD),
                Instant.parse("2026-10-01T00:00:00Z"));
        user.setFirstName("Nombre");
        user.setLastName("Apellido");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
