package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.CoachListItem;
import com.uniquindio.teen_sport.dto.GuardianListItem;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDirectoryServiceTest {

    private final CoachRepository coachRepository = mock(CoachRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final UserDirectoryService service = new UserDirectoryService(coachRepository, guardianRepository);

    @Test
    void listsAllCoachesWhenNoStatusIsGiven() {
        Coach coach = coach(5, 50, EntityStatus.INACTIVE);
        when(coachRepository.findAllBy(UserDirectoryService.ORDER_BY_USER_NAME)).thenReturn(List.of(coach));

        List<CoachListItem> coaches = service.listCoaches(null);

        assertThat(coaches).containsExactly(
                new CoachListItem(5, 50, "Luis Pérez", "Fútbol", 3, EntityStatus.INACTIVE));
        verify(coachRepository, never()).findByUserStatus(any(), any());
    }

    @Test
    void filtersCoachesByStatus() {
        when(coachRepository.findByUserStatus(EntityStatus.ACTIVE, UserDirectoryService.ORDER_BY_USER_NAME))
                .thenReturn(List.of(coach(6, 60, EntityStatus.ACTIVE)));

        assertThat(service.listCoaches(EntityStatus.ACTIVE)).extracting(CoachListItem::id).containsExactly(6);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsGuardiansWithMinimalDataAndPageMetadata() {
        User user = user(70, RoleName.GUARDIAN, EntityStatus.ACTIVE);
        Guardian guardian = new Guardian(user, "Calle 1");
        ReflectionTestUtils.setField(guardian, "id", 7);
        Pageable pageable = PageRequest.of(0, 20);
        when(guardianRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(guardian), pageable, 1));

        PageResponse<GuardianListItem> page = service.listGuardians("luis", 0, 20);

        assertThat(page.content()).containsExactly(
                new GuardianListItem(7, 70, "Luis Pérez", "u70@teensport.test", "300"));
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    private static Coach coach(int id, int userId, EntityStatus status) {
        Coach coach = new Coach(user(userId, RoleName.COACH, status), "Fútbol", 3);
        ReflectionTestUtils.setField(coach, "id", id);
        return coach;
    }

    private static User user(int id, RoleName role, EntityStatus status) {
        User user = new User(new Role(role, null), "u" + id + "@teensport.test", "$2a$hash", Instant.now());
        user.setFirstName("Luis");
        user.setLastName("Pérez");
        user.setPhone("300");
        user.setStatus(status);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
