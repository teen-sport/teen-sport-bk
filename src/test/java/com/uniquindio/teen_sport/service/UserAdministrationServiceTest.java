package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CreateUserRequest;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.dto.UpdateUserRequest;
import com.uniquindio.teen_sport.dto.UserProfile;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.mapper.UserProfileMapper;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdministrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-11T15:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final CoachRepository coachRepository = mock(CoachRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private UserAdministrationService service;

    @BeforeEach
    void setUp() {
        AccountCreator accountCreator = new AccountCreator(userRepository, roleRepository,
                new BCryptPasswordEncoder(4), new PasswordPolicy(), Clock.fixed(NOW, ZoneOffset.UTC));
        service = new UserAdministrationService(userRepository, coachRepository, groupRepository, accountCreator,
                new RoleProfileResolver(guardianRepository, coachRepository), new ProfileEditor(),
                new UserProfileMapper());
        for (RoleName name : RoleName.values()) {
            when(roleRepository.findByName(name)).thenReturn(Optional.of(new Role(name, null)));
        }
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(coachRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsCoachWithCoachProfile() {
        UserProfile profile = service.createStaffUser(createRequest(RoleName.COACH, " Fútbol ", 4));

        ArgumentCaptor<Coach> coach = ArgumentCaptor.forClass(Coach.class);
        verify(coachRepository).save(coach.capture());
        assertThat(coach.getValue().getSpecialty()).isEqualTo("Fútbol");
        assertThat(coach.getValue().getYearsExperience()).isEqualTo(4);
        assertThat(profile.role()).isEqualTo(RoleName.COACH);
        assertThat(profile.coach().specialty()).isEqualTo("Fútbol");
        assertThat(profile.acceptedDataPolicy()).isFalse();
    }

    @Test
    void createsAdminWithoutCoachProfileIgnoringCoachFields() {
        UserProfile profile = service.createStaffUser(createRequest(RoleName.ADMIN, "Fútbol", 4));

        verify(coachRepository, never()).save(any());
        assertThat(profile.role()).isEqualTo(RoleName.ADMIN);
        assertThat(profile.coach()).isNull();
        assertThat(profile.guardian()).isNull();
    }

    @Test
    void staffCreationRejectsDuplicatesAndWeakPasswords() {
        when(userRepository.existsByEmail("staff@teensport.test")).thenReturn(true);

        assertThat(failure(() -> service.createStaffUser(createRequest(RoleName.COACH, null, null))).getCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

        CreateUserRequest weak = new CreateUserRequest(RoleName.ADMIN, "A", "B", "CC", "1", "x@teensport.test",
                "300", "debil2026", null, null);
        assertThat(failure(() -> service.createStaffUser(weak)).getCode()).isEqualTo(ErrorCode.WEAK_PASSWORD);
    }

    @Test
    void missingUserIsNotFound() {
        when(userRepository.findById(5)).thenReturn(Optional.empty());

        BusinessException ex = failure(() -> service.getUser(5));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateChangesEmailWhenItIsNewAndAvailable() {
        User user = existing(7, RoleName.ADMIN, "old@teensport.test");

        UserProfile profile = service.updateUser(7, updateRequest(" New@TeenSport.test "));

        assertThat(user.getEmail()).isEqualTo("new@teensport.test");
        assertThat(profile.email()).isEqualTo("new@teensport.test");
        assertThat(profile.role()).isEqualTo(RoleName.ADMIN);
    }

    @Test
    void updateRejectsEmailOfAnotherAccount() {
        User user = existing(7, RoleName.ADMIN, "old@teensport.test");
        when(userRepository.existsByEmail("taken@teensport.test")).thenReturn(true);

        assertThat(failure(() -> service.updateUser(7, updateRequest("taken@teensport.test"))).getCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
        assertThat(user.getEmail()).isEqualTo("old@teensport.test");
    }

    @Test
    void updateKeepsEmailWhenOmittedOrUnchanged() {
        User user = existing(7, RoleName.ADMIN, "old@teensport.test");

        service.updateUser(7, updateRequest(null));
        service.updateUser(7, updateRequest("OLD@teensport.test"));

        assertThat(user.getEmail()).isEqualTo("old@teensport.test");
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateAppliesGuardianFields() {
        User user = existing(8, RoleName.GUARDIAN, "g@teensport.test");
        Guardian guardian = new Guardian(user, "Calle 1");
        when(guardianRepository.findByUserId(8)).thenReturn(Optional.of(guardian));

        service.updateUser(8, new UpdateUserRequest("Ana", "Gómez", null, "300", "Carrera 2", "Fútbol", 3));

        assertThat(guardian.getAddress()).isEqualTo("Carrera 2");
        assertThat(user.getFirstName()).isEqualTo("Ana");
    }

    @Test
    void adminCannotDeactivateOwnAccount() {
        User user = existing(1, RoleName.ADMIN, "admin@teensport.test");

        BusinessException ex = failure(() -> service.changeStatus(1, 1, EntityStatus.INACTIVE));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.CANNOT_DEACTIVATE_SELF);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(user.getStatus()).isEqualTo(EntityStatus.ACTIVE);
    }

    @Test
    void coachWithActiveGroupsCannotBeDeactivated() {
        User coachUser = existing(9, RoleName.COACH, "c@teensport.test");
        Coach coach = new Coach(coachUser, null, null);
        ReflectionTestUtils.setField(coach, "id", 90);
        when(coachRepository.findByUserId(9)).thenReturn(Optional.of(coach));
        when(groupRepository.existsByCoachIdAndStatus(90, EntityStatus.ACTIVE)).thenReturn(true);

        BusinessException ex = failure(() -> service.changeStatus(1, 9, EntityStatus.INACTIVE));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.COACH_HAS_ACTIVE_GROUPS);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(coachUser.getStatus()).isEqualTo(EntityStatus.ACTIVE);
    }

    @Test
    void coachWithoutActiveGroupsCanBeDeactivated() {
        User coachUser = existing(9, RoleName.COACH, "c@teensport.test");
        Coach coach = new Coach(coachUser, null, null);
        ReflectionTestUtils.setField(coach, "id", 90);
        when(coachRepository.findByUserId(9)).thenReturn(Optional.of(coach));

        service.changeStatus(1, 9, EntityStatus.INACTIVE);

        assertThat(coachUser.getStatus()).isEqualTo(EntityStatus.INACTIVE);
    }

    @Test
    void adminCanReactivateOwnAccountAndDeactivateOthers() {
        User self = existing(1, RoleName.ADMIN, "admin@teensport.test");
        User other = existing(2, RoleName.GUARDIAN, "g@teensport.test");

        service.changeStatus(1, 1, EntityStatus.ACTIVE);
        UserProfile profile = service.changeStatus(1, 2, EntityStatus.INACTIVE);

        assertThat(self.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(other.getStatus()).isEqualTo(EntityStatus.INACTIVE);
        assertThat(profile.status()).isEqualTo(EntityStatus.INACTIVE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsesOneBatchLookupForRoleProfilesAndKeepsPageMetadata() {
        User guardianUser = existing(3, RoleName.GUARDIAN, "g@teensport.test");
        User coachUser = existing(4, RoleName.COACH, "c@teensport.test");
        Guardian guardian = new Guardian(guardianUser, "Calle 1");
        Coach coach = new Coach(coachUser, "Fútbol", 2);
        Pageable pageable = PageRequest.of(1, 2, UserAdministrationService.USER_ORDER);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(guardianUser, coachUser), pageable, 7));
        when(guardianRepository.findByUserIdIn(anyCollection())).thenReturn(List.of(guardian));
        when(coachRepository.findByUserIdIn(anyCollection())).thenReturn(List.of(coach));

        PageResponse<UserProfile> page = service.listUsers(null, null, "ana", 1, 2);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(7);
        assertThat(page.totalPages()).isEqualTo(4);
        assertThat(page.content()).extracting(UserProfile::id).containsExactly(3, 4);
        assertThat(page.content().get(0).guardian().homeAddress()).isEqualTo("Calle 1");
        assertThat(page.content().get(1).coach().specialty()).isEqualTo("Fútbol");
        verify(guardianRepository, never()).findByUserId(any());
        verify(coachRepository, never()).findByUserId(any());
    }

    @Test
    void onlyStaffRolesAreAcceptedByTheCreationRequest() {
        assertThat(createRequest(RoleName.ADMIN, null, null).isStaffRole()).isTrue();
        assertThat(createRequest(RoleName.COACH, null, null).isStaffRole()).isTrue();
        assertThat(createRequest(RoleName.GUARDIAN, null, null).isStaffRole()).isFalse();
        assertThat(createRequest(RoleName.ADMIN, null, null).toString()).doesNotContain("Segura#2026");
    }

    private User existing(int id, RoleName role, String email) {
        User user = new User(new Role(role, null), email, "$2a$hash", NOW);
        user.setDocumentType("CC");
        user.setDocumentNumber("10" + id);
        user.setFirstName("Nombre");
        user.setLastName("Apellido");
        user.setPhone("300");
        ReflectionTestUtils.setField(user, "id", id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private static CreateUserRequest createRequest(RoleName role, String specialty, Integer years) {
        return new CreateUserRequest(role, "Luis", "Pérez", "CC", "123", "staff@teensport.test", "300",
                "Segura#2026", specialty, years);
    }

    private static UpdateUserRequest updateRequest(String email) {
        return new UpdateUserRequest("Nombre", "Apellido", email, "300", null, null, null);
    }

    private static BusinessException failure(Runnable action) {
        return catchThrowableOfType(BusinessException.class, action::run);
    }
}
