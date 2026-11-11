package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.NotificationType;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static com.uniquindio.teen_sport.service.SessionFixtures.NOW;
import static com.uniquindio.teen_sport.service.SessionFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Composición y destinatarios de las notificaciones.
 */
class NotificationServiceTest {

    private static final YearMonth PERIOD = YearMonth.of(2026, 11);

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final NotificationService service = new NotificationService(publisher, enrollmentRepository);
    private Group group;
    private Athlete athlete;
    private User guardian;

    @BeforeEach
    void setUp() {
        group = SessionFixtures.group();
        athlete = SessionFixtures.athlete(5, "Sofía", "Álvarez");
        guardian = athlete.getGuardian().getUser();
        ReflectionTestUtils.setField(guardian, "id", 30);
    }

    @Test
    void welcomesTheNewGuardianAfterCommit() {
        service.accountRegistered(guardian);

        NotificationRequested event = published();
        assertThat(event.userId()).isEqualTo(30);
        assertThat(event.type()).isEqualTo(NotificationType.ACCOUNT_REGISTERED);
        assertThat(event.afterRollback()).isFalse();
        assertThat(event.message().toEmail()).isEqualTo("g@teensport.test");
        assertThat(event.message().toName()).isEqualTo("Laura Ríos");
        assertThat(event.message().subject()).isEqualTo("Bienvenido a TeenSport");
        assertThat(event.message().htmlContent()).contains("Hola, Laura R&iacute;os:");
    }

    @Test
    void enrollmentDecisionsGoToTheAthletesGuardianWithTheReason() {
        Enrollment enrollment = new Enrollment(athlete, group, PERIOD, NOW);
        service.enrollmentConfirmed(enrollment);
        enrollment.setReviewNotes("Falta el certificado <médico>");
        service.enrollmentRejected(enrollment);

        ArgumentCaptor<NotificationRequested> events = ArgumentCaptor.forClass(NotificationRequested.class);
        verify(publisher, times(2)).publishEvent(events.capture());
        NotificationRequested confirmed = events.getAllValues().get(0);
        assertThat(confirmed.type()).isEqualTo(NotificationType.ENROLLMENT_CONFIRMED);
        assertThat(confirmed.message().htmlContent())
                .contains("Sof&iacute;a &Aacute;lvarez", "Grupo 7", "F&uacute;tbol", "noviembre de 2026");
        NotificationRequested rejected = events.getAllValues().get(1);
        assertThat(rejected.type()).isEqualTo(NotificationType.ENROLLMENT_REJECTED);
        assertThat(rejected.message().htmlContent())
                .contains("Motivo: Falta el certificado &lt;m&eacute;dico&gt;")
                .doesNotContain("<médico>");
    }

    @Test
    void noSpotsIsSentWhenTheRequestIsRolledBack() {
        service.noSpotsAvailable(athlete, group, PERIOD);

        NotificationRequested event = published();
        assertThat(event.type()).isEqualTo(NotificationType.NO_SPOTS_AVAILABLE);
        assertThat(event.afterRollback()).isTrue();
        assertThat(event.message().subject()).isEqualTo("Sin cupos disponibles");
    }

    @Test
    void inactiveAccountsReceiveNoEmails() {
        guardian.setStatus(EntityStatus.INACTIVE);

        service.accountRegistered(guardian);
        service.noSpotsAvailable(athlete, group, PERIOD);

        verifyNoInteractions(publisher);
    }

    @Test
    void publishedSchedulesGoOnceToEachGuardianOfTheGroup() {
        User other = account(31, "Pedro", "Gil");
        when(enrollmentRepository.findActiveGuardianAccounts(7)).thenReturn(List.of(guardian, other));
        List<TrainingSession> created = List.of(SessionFixtures.session(1, group, TODAY.plusDays(7)),
                SessionFixtures.session(2, group, TODAY), SessionFixtures.session(3, group, TODAY.plusDays(3)));

        service.schedulePublished(group, created);

        ArgumentCaptor<NotificationRequested> events = ArgumentCaptor.forClass(NotificationRequested.class);
        verify(publisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues()).extracting(NotificationRequested::userId).containsExactly(30, 31);
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.type()).isEqualTo(NotificationType.SCHEDULE_PUBLISHED);
            assertThat(event.message().htmlContent())
                    .contains("3 sesiones", "entre el 12 de octubre de 2026 y el 19 de octubre de 2026");
        });
        assertThat(events.getAllValues().get(1).message().htmlContent()).contains("Hola, Pedro Gil:");
    }

    @Test
    void aSingleSessionIsWrittenInSingular() {
        when(enrollmentRepository.findActiveGuardianAccounts(7)).thenReturn(List.of(guardian));

        service.schedulePublished(group, List.of(SessionFixtures.session(1, group, TODAY)));

        assertThat(published().message().htmlContent())
                .contains("Se programó 1 sesión del grupo", "entre el 12 de octubre de 2026 y el 12 de octubre de 2026");
    }

    @Test
    void withoutCreatedSessionsNothingIsPublished() {
        service.schedulePublished(group, List.of());

        verifyNoInteractions(publisher, enrollmentRepository);
    }

    @Test
    void sessionChangesDescribeTheMoveAndTheCancellation() {
        when(enrollmentRepository.findActiveGuardianAccounts(7)).thenReturn(List.of(guardian));
        TrainingSession session = SessionFixtures.session(5, group, TODAY.plusDays(1));
        service.sessionRescheduled(session, TODAY, LocalTime.of(15, 0));
        session.setNotes("Lluvia");
        service.sessionCancelled(session);

        ArgumentCaptor<NotificationRequested> events = ArgumentCaptor.forClass(NotificationRequested.class);
        verify(publisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues()).extracting(NotificationRequested::type)
                .containsOnly(NotificationType.SCHEDULE_CHANGE);
        assertThat(events.getAllValues().get(0).message().htmlContent())
                .contains("del 12 de octubre de 2026 a las 15:00 se trasladó al 13 de octubre de 2026, "
                        + "de 16:00 a 17:00");
        assertThat(events.getAllValues().get(1).message().htmlContent())
                .contains("fue cancelada. Motivo: Lluvia");
    }

    @Test
    void aGroupWithoutGuardiansSendsNothing() {
        when(enrollmentRepository.findActiveGuardianAccounts(7)).thenReturn(List.of());

        service.sessionCancelled(SessionFixtures.session(5, group, TODAY));

        verify(publisher, never()).publishEvent(any(Object.class));
    }

    private NotificationRequested published() {
        ArgumentCaptor<NotificationRequested> event = ArgumentCaptor.forClass(NotificationRequested.class);
        verify(publisher).publishEvent(event.capture());
        return event.getValue();
    }

    private static User account(int id, String firstName, String lastName) {
        User user = new User(new Role(RoleName.GUARDIAN, null), firstName.toLowerCase() + "@teensport.test",
                "$2a$hash", NOW);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
