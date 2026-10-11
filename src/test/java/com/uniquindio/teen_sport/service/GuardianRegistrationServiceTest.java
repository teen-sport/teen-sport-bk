package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.RegisterGuardianRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.UserProfileMapper;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuardianRegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-11T15:00:00Z");
    private static final String PASSWORD = "Segura#2026";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private GuardianRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new GuardianRegistrationService(userRepository, guardianRepository, roleRepository,
                passwordEncoder, new PasswordPolicy(), new UserProfileMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        when(roleRepository.findByName(RoleName.GUARDIAN))
                .thenReturn(Optional.of(new Role(RoleName.GUARDIAN, "Acudiente")));
        when(guardianRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registersActiveGuardianWithHashedPasswordAndPolicyAcceptance() {
        UserProfile profile = service.register(request(" Ana@TeenSport.TEST ", PASSWORD, true, "  "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertThat(user.getEmail()).isEqualTo("ana@teensport.test");
        assertThat(user.getRole().getName()).isEqualTo(RoleName.GUARDIAN);
        assertThat(user.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(user.isAcceptedDataPolicy()).isTrue();
        assertThat(user.getAcceptedDataPolicyAt()).isEqualTo(NOW);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
        assertThat(user.getDocumentNumber()).isEqualTo("123456789");

        ArgumentCaptor<Guardian> guardian = ArgumentCaptor.forClass(Guardian.class);
        verify(guardianRepository).save(guardian.capture());
        assertThat(guardian.getValue().getUser()).isSameAs(user);
        assertThat(guardian.getValue().getAddress()).isNull();

        assertThat(profile.role()).isEqualTo(RoleName.GUARDIAN);
        assertThat(profile.acceptedDataPolicy()).isTrue();
        assertThat(profile.guardian()).isNotNull();
        assertThat(profile.coach()).isNull();
    }

    @Test
    void rejectsRegistrationWithoutDataPolicyAcceptance() {
        BusinessException ex = failure(request("ana@teensport.test", PASSWORD, false, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_POLICY_NOT_ACCEPTED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsWeakPassword() {
        assertThat(failure(request("ana@teensport.test", "segura2026", true, null)).getCode())
                .isEqualTo(ErrorCode.WEAK_PASSWORD);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsEmailAlreadyRegisteredComparingNormalizedEmail() {
        when(userRepository.existsByEmail("ana@teensport.test")).thenReturn(true);

        BusinessException ex = failure(request("ANA@teensport.test", PASSWORD, true, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsIdNumberAlreadyRegistered() {
        when(userRepository.existsByDocumentNumber("123456789")).thenReturn(true);

        BusinessException ex = failure(request("ana@teensport.test", PASSWORD, true, null));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.ID_NUMBER_ALREADY_REGISTERED);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void missingGuardianRoleIsAConfigurationFailure() {
        when(roleRepository.findByName(RoleName.GUARDIAN)).thenReturn(Optional.empty());

        assertThat(catchThrowableOfType(IllegalStateException.class,
                () -> service.register(request("ana@teensport.test", PASSWORD, true, null)))).isNotNull();
    }

    private BusinessException failure(RegisterGuardianRequest request) {
        return catchThrowableOfType(BusinessException.class, () -> service.register(request));
    }

    private static RegisterGuardianRequest request(String email, String password, boolean accepted, String address) {
        return new RegisterGuardianRequest("Ana", "Prueba", "CC", " 123456789 ", email, "3001234567", address,
                password, accepted);
    }
}
