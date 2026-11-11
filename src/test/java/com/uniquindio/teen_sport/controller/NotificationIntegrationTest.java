package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.integration.EmailMessage;
import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient.TestCoach;
import com.uniquindio.teen_sport.support.AuthTestClient.TestGuardian;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.RecordingEmailSender;
import com.uniquindio.teen_sport.support.TestAccounts;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notificaciones por correo de punta a punta: el caso de uso, el registro en {@code notifications}
 * (PostgreSQL) y el proveedor en memoria. Ninguna prueba llama a Brevo.
 */
@IntegrationTest
class NotificationIntegrationTest {

    private static final ZoneId ACADEMY_ZONE = ZoneId.of("America/Bogota");
    private static final String ENROLLMENTS = "/api/v1/enrollments";
    private static final String SESSIONS = "/api/v1/sessions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingEmailSender emailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AuthTestClient auth;
    private AcademyTestClient academy;
    private String adminToken;
    private TestCoach coach;
    private int disciplineId;
    private String period;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
        academy = new AcademyTestClient(mockMvc, adminToken);
        coach = auth.newCoach();
        disciplineId = academy.newDiscipline();
        period = YearMonth.now(ACADEMY_ZONE).toString();
    }

    @AfterEach
    void acceptAllEmails() {
        emailSender.acceptAll();
    }

    @Test
    void registrationSendsTheWelcomeEmailAndRecordsItAsSent() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        String email = emailOf(guardian.userId());

        assertThat(emailSender.sentTo(email)).singleElement()
                .satisfies(message -> {
                    assertThat(message.subject()).isEqualTo("Bienvenido a TeenSport");
                    assertThat(message.toName()).isEqualTo("Ana María Prueba Test");
                });
        assertThat(notifications(guardian.userId())).containsExactly("ACCOUNT_REGISTERED SENT 1");
    }

    @Test
    void aProviderOutageDoesNotUndoTheRegistrationAndEndsAsFailedAfterThreeAttempts() throws Exception {
        String email = TestAccounts.uniqueEmail();
        emailSender.failFor(email, true);

        int userId = register(email);

        assertThat(emailSender.sentTo(email)).isEmpty();
        assertThat(notifications(userId)).containsExactly("ACCOUNT_REGISTERED FAILED 3");
    }

    @Test
    void aPermanentProviderErrorIsNotRetried() throws Exception {
        String email = TestAccounts.uniqueEmail();
        emailSender.failFor(email, false);

        int userId = register(email);

        assertThat(notifications(userId)).containsExactly("ACCOUNT_REGISTERED FAILED 1");
    }

    @Test
    void aFullGroupWarnsTheGuardianEvenThoughTheRequestIsRolledBack() throws Exception {
        int groupId = academy.newGroup(disciplineId, coach.coachId(), 1);
        TestGuardian first = auth.newGuardian();
        academy.requestEnrollment(first.token(), academy.newAthlete(first.token()), groupId, period)
                .andExpect(status().isCreated());
        TestGuardian second = auth.newGuardian();

        academy.requestEnrollment(second.token(), academy.newAthlete(second.token()), groupId, period)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_AVAILABLE_SPOTS"));

        assertThat(notifications(second.userId())).containsExactly("ACCOUNT_REGISTERED SENT 1",
                "NO_SPOTS_AVAILABLE SENT 1");
        assertThat(emailSender.sentTo(emailOf(second.userId()))).extracting(EmailMessage::subject)
                .containsExactly("Bienvenido a TeenSport", "Sin cupos disponibles");
        assertThat(notifications(first.userId())).containsExactly("ACCOUNT_REGISTERED SENT 1");
    }

    @Test
    void confirmingAndRejectingNotifyTheGuardianOnlyWhenTheOperationSucceeds() throws Exception {
        int groupId = academy.newGroup(disciplineId, coach.coachId());
        TestGuardian guardian = auth.newGuardian();
        int confirmed = enrollment(guardian, groupId);
        int rejected = enrollment(guardian, academy.newGroup(academy.newDiscipline(), coach.coachId()));

        send(post(ENROLLMENTS + "/" + confirmed + "/confirm"), null).andExpect(status().isOk());
        send(post(ENROLLMENTS + "/" + rejected + "/reject"), "{\"reason\":\"Cupo reservado <para> otro\"}")
                .andExpect(status().isOk());
        send(post(ENROLLMENTS + "/" + rejected + "/reject"), "{\"reason\":\"Otra vez\"}")
                .andExpect(status().isConflict());

        assertThat(notifications(guardian.userId())).containsExactly("ACCOUNT_REGISTERED SENT 1",
                "ENROLLMENT_CONFIRMED SENT 1", "ENROLLMENT_REJECTED SENT 1");
        List<EmailMessage> messages = emailSender.sentTo(emailOf(guardian.userId()));
        assertThat(messages.get(2).htmlContent()).contains("Motivo: Cupo reservado &lt;para&gt; otro");
    }

    @Test
    void scheduleNewsReachEachGuardianWithAnActiveEnrollmentOnce() throws Exception {
        LocalDate monday = LocalDate.now(ACADEMY_ZONE).plusWeeks(1).with(DayOfWeek.MONDAY);
        int groupId = academy.newGroup(disciplineId, coach.coachId());
        academy.addSchedule(groupId, "MONDAY", "16:00", "17:00").andExpect(status().isCreated());
        TestGuardian enrolled = auth.newGuardian();
        confirm(enrollment(enrolled, groupId));
        confirm(enrollment(enrolled, groupId));
        TestGuardian pending = auth.newGuardian();
        enrollment(pending, groupId);

        send(post(SESSIONS + "/generate"), generateJson(groupId, monday, monday.plusDays(13), false))
                .andExpect(status().isCreated());
        String generated = send(post(SESSIONS + "/generate"), generateJson(groupId, monday, monday.plusDays(20), true))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int sessionId = JsonPath.read(generated, "$.sessions[0].id");
        send(put(SESSIONS + "/" + sessionId), "{\"date\":\"%s\",\"startTime\":\"18:00\",\"endTime\":\"19:00\"}"
                .formatted(monday.plusDays(15))).andExpect(status().isOk());
        send(post(SESSIONS + "/" + sessionId + "/cancel"), "{\"reason\":\"Lluvia\"}").andExpect(status().isOk());

        assertThat(notifications(enrolled.userId())).containsExactly("ACCOUNT_REGISTERED SENT 1",
                "ENROLLMENT_CONFIRMED SENT 1", "ENROLLMENT_CONFIRMED SENT 1", "SCHEDULE_PUBLISHED SENT 1",
                "SCHEDULE_CHANGE SENT 1", "SCHEDULE_CHANGE SENT 1");
        assertThat(notifications(pending.userId())).containsExactly("ACCOUNT_REGISTERED SENT 1");
        List<EmailMessage> messages = emailSender.sentTo(emailOf(enrolled.userId()));
        assertThat(messages.get(3).htmlContent()).contains("Se programó 1 sesión del grupo");
        assertThat(messages.get(5).htmlContent()).contains("fue cancelada. Motivo: Lluvia");
    }

    private int register(String email) throws Exception {
        String profile = mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(),
                                TestAccounts.VALID_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(profile, "$.id");
    }

    private int enrollment(TestGuardian guardian, int groupId) throws Exception {
        String response = academy.requestEnrollment(guardian.token(), academy.newAthlete(guardian.token()), groupId,
                        period)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void confirm(int enrollmentId) throws Exception {
        send(post(ENROLLMENTS + "/" + enrollmentId + "/confirm"), null).andExpect(status().isOk());
    }

    /**
     * @return cada notificación del usuario como {@code "TIPO ESTADO INTENTOS"}, en orden de creación
     */
    private List<String> notifications(int userId) {
        return jdbcTemplate.queryForList("""
                select type || ' ' || delivery_status || ' ' || attempts from notifications
                where user_id = ? order by id
                """, String.class, userId);
    }

    private String emailOf(int userId) {
        return jdbcTemplate.queryForObject("select email from users where id = ?", String.class, userId);
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(adminToken));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static String generateJson(int groupId, LocalDate from, LocalDate to, boolean notify) {
        return "{\"groupId\":%d,\"dateFrom\":\"%s\",\"dateTo\":\"%s\",\"notify\":%s}"
                .formatted(groupId, from, to, notify);
    }
}
