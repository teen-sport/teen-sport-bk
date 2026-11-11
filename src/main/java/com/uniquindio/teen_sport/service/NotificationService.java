package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.NotificationType;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.integration.EmailMessage;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Notificaciones por correo de los casos de uso (catálogo aprobado el 2026-11-10).
 *
 * <p>Se llama dentro de la transacción del caso de uso: compone el correo con los datos ya cargados y
 * publica un {@link NotificationRequested}. El envío lo hace {@link NotificationDispatcher} cuando la
 * transacción termina, de modo que una operación revertida no envía correos de éxito y un fallo del
 * proveedor no revierte la operación.</p>
 *
 * <p>Las cuentas inactivas no reciben correos. Los avisos de horarios van a los acudientes activos con
 * algún deportista en matrícula {@code ACTIVE} en el grupo, una sola vez por acudiente.</p>
 */
@Service
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * @param eventPublisher       publicación de eventos de aplicación
     * @param enrollmentRepository matrículas, para hallar a los acudientes de un grupo
     */
    public NotificationService(ApplicationEventPublisher eventPublisher, EnrollmentRepository enrollmentRepository) {
        this.eventPublisher = eventPublisher;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * U2: bienvenida al acudiente registrado.
     *
     * @param user cuenta recién creada
     */
    public void accountRegistered(User user) {
        publish(user, NotificationType.ACCOUNT_REGISTERED, NotificationContent.accountRegistered(fullName(user)),
                false);
    }

    /**
     * E12: matrícula confirmada, al acudiente del deportista.
     *
     * @param enrollment matrícula ya confirmada, en su grupo final
     */
    public void enrollmentConfirmed(Enrollment enrollment) {
        User guardian = guardianOf(enrollment.getAthlete());
        Group group = enrollment.getGroup();
        publish(guardian, NotificationType.ENROLLMENT_CONFIRMED, NotificationContent.enrollmentConfirmed(
                fullName(guardian), fullName(enrollment.getAthlete()), group.getName(),
                group.getDiscipline().getName(), enrollment.getPeriod()), false);
    }

    /**
     * E13: matrícula rechazada, con el motivo, al acudiente del deportista.
     *
     * @param enrollment matrícula ya rechazada, con el motivo en sus notas de revisión
     */
    public void enrollmentRejected(Enrollment enrollment) {
        User guardian = guardianOf(enrollment.getAthlete());
        Group group = enrollment.getGroup();
        publish(guardian, NotificationType.ENROLLMENT_REJECTED, NotificationContent.enrollmentRejected(
                fullName(guardian), fullName(enrollment.getAthlete()), group.getName(),
                group.getDiscipline().getName(), enrollment.getReviewNotes()), false);
    }

    /**
     * E6: solicitud rechazada por falta de cupo. Se envía aunque la transacción se revierta, porque
     * informa precisamente de la solicitud que no se registró.
     *
     * @param athlete deportista de la solicitud
     * @param group   grupo sin cupo
     * @param period  mes de inicio pedido
     */
    public void noSpotsAvailable(Athlete athlete, Group group, YearMonth period) {
        User guardian = guardianOf(athlete);
        publish(guardian, NotificationType.NO_SPOTS_AVAILABLE, NotificationContent.noSpotsAvailable(
                fullName(guardian), fullName(athlete), group.getName(), group.getDiscipline().getName(), period),
                true);
    }

    /**
     * A2 con {@code notify}: sesiones nuevas del grupo. Sin sesiones creadas no se avisa.
     *
     * @param group   grupo, con su disciplina
     * @param created sesiones creadas
     */
    public void schedulePublished(Group group, List<TrainingSession> created) {
        if (created.isEmpty()) {
            return;
        }
        LocalDate from = created.stream().map(TrainingSession::getSessionDate).min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate to = created.stream().map(TrainingSession::getSessionDate).max(Comparator.naturalOrder())
                .orElseThrow();
        notifyGroupGuardians(group, NotificationType.SCHEDULE_PUBLISHED, guardian ->
                NotificationContent.schedulePublished(fullName(guardian), group.getName(),
                        group.getDiscipline().getName(), created.size(), from, to));
    }

    /**
     * A5: sesión reprogramada.
     *
     * @param session       sesión con su nueva fecha y horas
     * @param previousDate  fecha anterior
     * @param previousStart hora de inicio anterior
     */
    public void sessionRescheduled(TrainingSession session, LocalDate previousDate, LocalTime previousStart) {
        Group group = session.getGroup();
        notifyGroupGuardians(group, NotificationType.SCHEDULE_CHANGE, guardian ->
                NotificationContent.sessionRescheduled(fullName(guardian), group.getName(), previousDate,
                        previousStart, session.getSessionDate(), session.getStartTime(), session.getEndTime()));
    }

    /**
     * A6: sesión cancelada, con el motivo.
     *
     * @param session sesión cancelada, con el motivo en sus notas
     */
    public void sessionCancelled(TrainingSession session) {
        Group group = session.getGroup();
        notifyGroupGuardians(group, NotificationType.SCHEDULE_CHANGE, guardian ->
                NotificationContent.sessionCancelled(fullName(guardian), group.getName(), session.getSessionDate(),
                        session.getStartTime(), session.getNotes()));
    }

    /**
     * U13: enlace de recuperación de contraseña. El enlace lleva el token en claro: solo viaja en el
     * correo y no se guarda en {@code notifications}.
     *
     * @param user     cuenta que pidió la recuperación
     * @param link     enlace a la página de restablecimiento, con el token
     * @param validity vigencia del token
     */
    public void passwordResetRequested(User user, String link, Duration validity) {
        publish(user, NotificationType.PASSWORD_RESET,
                NotificationContent.passwordReset(fullName(user), link, validity.toMinutes()), false);
    }

    private void notifyGroupGuardians(Group group, NotificationType type,
                                      Function<User, NotificationContent> content) {
        enrollmentRepository.findActiveGuardianAccounts(group.getId())
                .forEach(guardian -> publish(guardian, type, content.apply(guardian), false));
    }

    private void publish(User recipient, NotificationType type, NotificationContent content, boolean afterRollback) {
        if (!recipient.isActive()) {
            return;
        }
        EmailMessage message = new EmailMessage(recipient.getEmail(), fullName(recipient), content.subject(),
                content.html());
        eventPublisher.publishEvent(new NotificationRequested(recipient.getId(), type, message, afterRollback));
    }

    private static User guardianOf(Athlete athlete) {
        return athlete.getGuardian().getUser();
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static String fullName(Athlete athlete) {
        return athlete.getFirstName() + " " + athlete.getLastName();
    }
}
