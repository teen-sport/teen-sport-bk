package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.UserReport;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.RoleCount;
import com.uniquindio.teen_sport.repository.RoleStatusCount;
import com.uniquindio.teen_sport.repository.UserRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R5: informe de cuentas de usuario.
 */
class UserReportServiceTest {

    private static final YearMonth PERIOD = YearMonth.of(2026, 10);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final UserReportService service = new UserReportService(userRepository, guardianRepository,
            SessionFixtures.calendar());

    @Test
    void countsRegistrationsStatusesAndTheGuardianLinkRate() {
        List<RoleCount> registered = List.of(roleCount(RoleName.GUARDIAN, 6), roleCount(RoleName.COACH, 1));
        when(userRepository.countCreatedByRole(any(), any())).thenReturn(registered);
        List<RoleStatusCount> statuses = List.of(statusCount(RoleName.ADMIN, EntityStatus.ACTIVE, 2),
                statusCount(RoleName.COACH, EntityStatus.ACTIVE, 6), statusCount(RoleName.COACH, EntityStatus.INACTIVE, 1),
                statusCount(RoleName.GUARDIAN, EntityStatus.ACTIVE, 3), statusCount(RoleName.GUARDIAN, EntityStatus.INACTIVE, 2));
        when(userRepository.countByRoleAndStatus()).thenReturn(statuses);
        when(guardianRepository.countActiveWithAthletes()).thenReturn(2L);

        UserReport report = service.userReport(PERIOD);

        assertThat(report.period()).isEqualTo(PERIOD);
        assertThat(report.registeredInPeriod()).isEqualTo(7);
        assertThat(report.inactiveAccounts()).isEqualTo(3);
        assertThat(report.guardiansWithAthletes()).isEqualTo(2);
        assertThat(report.guardianLinkRate()).isEqualTo(66.7);
        assertThat(report.byRole()).containsExactly(
                new UserReport.RoleAccounts(RoleName.ADMIN, 0, 2, 0),
                new UserReport.RoleAccounts(RoleName.COACH, 1, 6, 1),
                new UserReport.RoleAccounts(RoleName.GUARDIAN, 6, 3, 2));
    }

    @Test
    void thePeriodIsTheMonthInTheAcademyTimeZone() {
        when(userRepository.countCreatedByRole(any(), any())).thenReturn(List.of());

        service.userReport(PERIOD);

        verify(userRepository).countCreatedByRole(Instant.parse("2026-10-01T05:00:00Z"),
                Instant.parse("2026-11-01T05:00:00Z"));
    }

    @Test
    void withoutAccountsEveryRoleIsZeroAndTheRateIsZero() {
        when(userRepository.countCreatedByRole(any(), any())).thenReturn(List.of());
        when(userRepository.countByRoleAndStatus()).thenReturn(List.of());

        UserReport report = service.userReport(PERIOD);

        assertThat(report.registeredInPeriod()).isZero();
        assertThat(report.inactiveAccounts()).isZero();
        assertThat(report.guardianLinkRate()).isZero();
        assertThat(report.byRole()).extracting(UserReport.RoleAccounts::role)
                .containsExactly(RoleName.ADMIN, RoleName.COACH, RoleName.GUARDIAN);
        assertThat(report.byRole()).allSatisfy(role -> {
            assertThat(role.registeredInPeriod()).isZero();
            assertThat(role.active()).isZero();
            assertThat(role.inactive()).isZero();
        });
    }

    private static RoleCount roleCount(RoleName role, long total) {
        RoleCount count = mock(RoleCount.class);
        when(count.getRole()).thenReturn(role);
        when(count.getTotal()).thenReturn(total);
        return count;
    }

    private static RoleStatusCount statusCount(RoleName role, EntityStatus status, long total) {
        RoleStatusCount count = mock(RoleStatusCount.class);
        when(count.getRole()).thenReturn(role);
        when(count.getStatus()).thenReturn(status);
        when(count.getTotal()).thenReturn(total);
        return count;
    }
}
