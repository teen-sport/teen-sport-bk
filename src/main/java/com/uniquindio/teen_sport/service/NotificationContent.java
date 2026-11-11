package com.uniquindio.teen_sport.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.web.util.HtmlUtils;

/**
 * Asunto y cuerpo en español de cada notificación. Los datos variables (nombres, motivos) los escriben
 * los usuarios, así que se escapan antes de insertarlos en el HTML.
 *
 * @param subject asunto
 * @param html    cuerpo en HTML
 */
record NotificationContent(String subject, String html) {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", SPANISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", SPANISH);

    static NotificationContent accountRegistered(String name) {
        return of("Bienvenido a TeenSport", name,
                "Tu cuenta de acudiente en TeenSport fue creada. Ya puedes iniciar sesión para registrar a tus "
                        + "deportistas y solicitar sus matrículas.");
    }

    static NotificationContent enrollmentConfirmed(String name, String athlete, String group, String discipline,
                                                   YearMonth period) {
        return of("Matrícula confirmada", name,
                "La matrícula de " + escape(athlete) + " en el grupo " + escape(group) + " de "
                        + escape(discipline) + " fue confirmada desde " + month(period) + ".");
    }

    static NotificationContent enrollmentRejected(String name, String athlete, String group, String discipline,
                                                  String reason) {
        return of("Matrícula rechazada", name,
                "La solicitud de matrícula de " + escape(athlete) + " en el grupo " + escape(group) + " de "
                        + escape(discipline) + " fue rechazada. Motivo: " + escape(reason));
    }

    static NotificationContent noSpotsAvailable(String name, String athlete, String group, String discipline,
                                                YearMonth period) {
        return of("Sin cupos disponibles", name,
                "No se pudo registrar la solicitud de matrícula de " + escape(athlete) + " en el grupo "
                        + escape(group) + " de " + escape(discipline) + " desde " + month(period)
                        + ", porque el grupo no tiene cupos disponibles.");
    }

    static NotificationContent schedulePublished(String name, String group, String discipline, int sessions,
                                                 LocalDate from, LocalDate to) {
        String created = sessions == 1 ? "Se programó 1 sesión" : "Se programaron " + sessions + " sesiones";
        return of("Nuevos horarios de entrenamiento", name,
                created + " del grupo " + escape(group) + " de " + escape(discipline)
                        + " entre el " + date(from) + " y el " + date(to)
                        + ". Puedes consultar el detalle en TeenSport.");
    }

    static NotificationContent sessionRescheduled(String name, String group, LocalDate previousDate,
                                                  LocalTime previousStart, LocalDate date, LocalTime start,
                                                  LocalTime end) {
        return of("Cambio de horario", name,
                "La sesión del grupo " + escape(group) + " del " + date(previousDate) + " a las "
                        + time(previousStart) + " se trasladó al " + date(date) + ", de " + time(start) + " a "
                        + time(end) + ".");
    }

    static NotificationContent sessionCancelled(String name, String group, LocalDate date, LocalTime start,
                                                String reason) {
        return of("Sesión cancelada", name,
                "La sesión del grupo " + escape(group) + " del " + date(date) + " a las " + time(start)
                        + " fue cancelada. Motivo: " + escape(reason));
    }

    /**
     * @param body texto ya escapado
     */
    private static NotificationContent of(String subject, String name, String body) {
        return new NotificationContent(subject, "<p>Hola, " + escape(name) + ":</p><p>" + body
                + "</p><p>Equipo TeenSport</p>");
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text);
    }

    private static String date(LocalDate date) {
        return DATE.format(date);
    }

    private static String month(YearMonth period) {
        return MONTH.format(period);
    }

    private static String time(LocalTime time) {
        return TIME.format(time);
    }
}
