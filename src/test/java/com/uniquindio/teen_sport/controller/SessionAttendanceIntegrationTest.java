package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient.TestCoach;
import com.uniquindio.teen_sport.support.AuthTestClient.TestGuardian;
import com.uniquindio.teen_sport.support.ConcurrentRequests;
import com.uniquindio.teen_sport.support.IntegrationTest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU39: sesiones (A1–A6), asistencia (A7–A9) y su efecto en los grupos (H10, H11) contra la API real
 * con PostgreSQL: reglas de fecha y estado, permisos por rol y por propiedad, lista de asistencia según
 * el mes de inicio de la matrícula y operaciones simultáneas.
 */
@IntegrationTest
class SessionAttendanceIntegrationTest {

    private static final String SESSIONS = "/api/v1/sessions";
    private static final String RECORDS = "/api/v1/attendance-records";
    private static final ZoneId ACADEMY_ZONE = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TrainingSessionRepository sessionRepository;

    private AuthTestClient auth;
    private AcademyTestClient academy;
    private String adminToken;
    private TestCoach coach;
    private int groupId;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
        academy = new AcademyTestClient(mockMvc, adminToken);
        coach = auth.newCoach();
        groupId = academy.newGroup(academy.newDiscipline(), coach.coachId());
        today = LocalDate.now(ACADEMY_ZONE);
    }

    @Test
    void creatingASessionValidatesDatesTimesDuplicatesAndOwnership() throws Exception {
        LocalDate tomorrow = today.plusDays(1);

        send(post(SESSIONS), coach.token(), sessionJson(groupId, tomorrow, "10:00", "11:30", "\"notes\":\" Extra \""))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/api/v1/sessions/\\d+")))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.attendanceRecorded").value(false))
                .andExpect(jsonPath("$.coach.id").value(coach.coachId()))
                .andExpect(jsonPath("$.startTime").value("10:00"))
                .andExpect(jsonPath("$.notes").value("Extra"))
                .andExpect(jsonPath("$.athletes").doesNotExist());
        send(post(SESSIONS), adminToken, sessionJson(groupId, tomorrow, "10:00", "12:00", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SESSION"));
        send(post(SESSIONS), adminToken, sessionJson(groupId, today, "10:00", "10:00", null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
        send(post(SESSIONS), adminToken, sessionJson(groupId, today.minusDays(1), "10:00", "11:00", null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DATE_IN_PAST"));
        send(post(SESSIONS), adminToken, sessionJson(groupId, today, "06:00", "07:00", null))
                .andExpect(status().isCreated());
        send(post(SESSIONS), adminToken, sessionJson(999999999, today, "10:00", "11:00", null))
                .andExpect(status().isNotFound());
        send(post(SESSIONS), auth.newCoachToken(), sessionJson(groupId, tomorrow, "14:00", "15:00", null))
                .andExpect(status().isForbidden());
        send(post(SESSIONS), auth.newGuardianToken(), sessionJson(groupId, tomorrow, "14:00", "15:00", null))
                .andExpect(status().isForbidden());
        send(post(SESSIONS), adminToken, "{\"date\":\"" + tomorrow + "\",\"startTime\":\"10:00\",\"endTime\":\"11:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("groupId"));
        send(post(SESSIONS), adminToken, sessionJson(groupId, tomorrow, "25:00", "26:00", null))
                .andExpect(status().isBadRequest());

        int inactiveGroup = academy.newGroup(academy.newDiscipline(), coach.coachId());
        send(patch("/api/v1/groups/" + inactiveGroup + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(post(SESSIONS), adminToken, sessionJson(inactiveGroup, tomorrow, "10:00", "11:00", null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("GROUP_INACTIVE"));
    }

    @Test
    void generatingCreatesOneSessionPerSlotAndDateAndSkipsExistingOnes() throws Exception {
        LocalDate first = today.plusDays(2);
        academy.addSchedule(groupId, first.getDayOfWeek().name(), "16:00", "17:30").andExpect(status().isCreated());
        createSession(groupId, first, "16:00", "17:00");

        send(post(SESSIONS + "/generate"), coach.token(), generateJson(groupId, today.plusDays(1), today.plusDays(14)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.sessions[0].date").value(first.plusWeeks(1).toString()))
                .andExpect(jsonPath("$.sessions[0].endTime").value("17:30"));
        send(post(SESSIONS + "/generate"), adminToken, generateJson(groupId, today.plusDays(1), today.plusDays(14)))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(2));

        send(post(SESSIONS + "/generate"), adminToken, generateJson(groupId, today, today.plusDays(61)))
                .andExpect(status().isCreated());
        send(post(SESSIONS + "/generate"), adminToken, generateJson(groupId, today, today.plusDays(62)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
        send(post(SESSIONS + "/generate"), adminToken, generateJson(groupId, today.plusDays(3), today.plusDays(2)))
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
        send(post(SESSIONS + "/generate"), adminToken, generateJson(groupId, today.minusDays(1), today))
                .andExpect(jsonPath("$.code").value("DATE_IN_PAST"));
        int withoutSchedules = academy.newGroup(academy.newDiscipline(), coach.coachId());
        send(post(SESSIONS + "/generate"), adminToken, generateJson(withoutSchedules, today, today.plusDays(7)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("GROUP_WITHOUT_SCHEDULES"));
        send(post(SESSIONS + "/generate"), auth.newCoachToken(), generateJson(groupId, today, today.plusDays(7)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyScheduledSessionsCanBeRescheduledOrCancelled() throws Exception {
        int sessionId = createSession(groupId, today.plusDays(1), "10:00", "11:00");
        createSession(groupId, today.plusDays(2), "10:00", "11:00");

        send(put(SESSIONS + "/" + sessionId), coach.token(), updateJson(today.plusDays(2), "10:00", "11:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SESSION"));
        send(put(SESSIONS + "/" + sessionId), coach.token(), updateJson(today.plusDays(1), "12:00", "11:00"))
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
        send(put(SESSIONS + "/" + sessionId), coach.token(), updateJson(today.minusDays(1), "10:00", "11:00"))
                .andExpect(jsonPath("$.code").value("DATE_IN_PAST"));
        send(put(SESSIONS + "/" + sessionId), auth.newCoachToken(), updateJson(today.plusDays(3), "10:00", "11:00"))
                .andExpect(status().isForbidden());
        send(put(SESSIONS + "/" + sessionId), coach.token(), updateJson(today.plusDays(1), "10:00", "11:30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime").value("11:30"));

        send(post(SESSIONS + "/" + sessionId + "/cancel"), coach.token(), "{}")
                .andExpect(status().isBadRequest());
        send(post(SESSIONS + "/" + sessionId + "/cancel"), coach.token(), "{\"reason\":\" Lluvia \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.notes").value("Lluvia"));
        send(post(SESSIONS + "/" + sessionId + "/cancel"), adminToken, "{\"reason\":\"Otra vez\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        send(put(SESSIONS + "/" + sessionId), adminToken, updateJson(today.plusDays(4), "10:00", "11:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        send(post(SESSIONS + "/999999999/cancel"), adminToken, "{\"reason\":\"X\"}").andExpect(status().isNotFound());
    }

    @Test
    void attendanceCoversTheWholeRosterAndSavingAgainReplacesTheMarks() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        int first = activeAthlete(guardian.token(), YearMonth.from(today));
        int second = activeAthlete(guardian.token(), YearMonth.from(today));
        int nextMonth = activeAthlete(guardian.token(), YearMonth.from(today).plusMonths(1));
        int pending = academy.newAthlete(guardian.token());
        academy.requestEnrollment(guardian.token(), pending, groupId, YearMonth.from(today).toString())
                .andExpect(status().isCreated());
        int sessionId = createSession(groupId, today, "05:00", "06:00");
        String url = SESSIONS + "/" + sessionId + "/attendance";

        send(get(url), coach.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(2))
                .andExpect(jsonPath("$.records[0].status").value(nullValue()))
                .andExpect(jsonPath("$.attendanceRecorded").value(false));
        send(get(url), adminToken, null).andExpect(status().isOk());
        send(get(url), auth.newCoachToken(), null).andExpect(status().isForbidden());
        send(get(url), guardian.token(), null).andExpect(status().isForbidden());

        send(put(url), adminToken, marks(first, "PRESENT", second, "ABSENT")).andExpect(status().isForbidden());
        send(put(url), auth.newCoachToken(), marks(first, "PRESENT", second, "ABSENT")).andExpect(status().isForbidden());
        send(put(url), coach.token(), "{\"records\":[{\"athleteId\":" + first + ",\"status\":\"PRESENT\"}]}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INCOMPLETE_ATTENDANCE"));
        send(put(url), coach.token(), marks(first, "PRESENT", nextMonth, "ABSENT"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ATHLETE_NOT_IN_ROSTER"));
        send(put(url), coach.token(), marks(first, "PRESENT", first, "ABSENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("records"));
        send(put(url), coach.token(), "{\"records\":[]}").andExpect(status().isBadRequest());

        send(put(url), coach.token(), marks(first, "PRESENT", second, "ABSENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.attendanceRecorded").value(true));
        send(put(url), coach.token(), marks(first, "ABSENT", second, "ABSENT"))
                .andExpect(status().isOk());

        send(get(RECORDS + "?groupId=" + groupId), guardian.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].status", not(hasItem("PRESENT"))))
                .andExpect(jsonPath("$.content[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$.content[0].athlete.lastNames").isNotEmpty())
                .andExpect(jsonPath("$.content[0].recordedAt").isNotEmpty());
        send(get(RECORDS + "?groupId=" + groupId + "&athleteId=" + first + "&status=ABSENT&from=" + today
                + "&to=" + today), coach.token(), null)
                .andExpect(jsonPath("$.totalElements").value(1));
        send(get(RECORDS + "?groupId=" + groupId), adminToken, null).andExpect(jsonPath("$.totalElements").value(2));
        send(get(RECORDS + "?groupId=" + groupId), auth.newGuardianToken(), null)
                .andExpect(jsonPath("$.totalElements").value(0));
        send(get(RECORDS + "?groupId=" + groupId), auth.newCoachToken(), null)
                .andExpect(jsonPath("$.totalElements").value(0));
        send(get(RECORDS + "?from=" + today + "&to=" + today.minusDays(1)), adminToken, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

        send(put(SESSIONS + "/" + sessionId), coach.token(), updateJson(today.plusDays(1), "05:00", "06:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        int future = createSession(groupId, today.plusDays(1), "05:00", "06:00");
        send(put(SESSIONS + "/" + future + "/attendance"), coach.token(), marks(first, "PRESENT", second, "ABSENT"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("FUTURE_SESSION"));
        int cancelled = createSession(groupId, today, "07:00", "08:00");
        send(post(SESSIONS + "/" + cancelled + "/cancel"), coach.token(), "{\"reason\":\"Lluvia\"}")
                .andExpect(status().isOk());
        send(put(SESSIONS + "/" + cancelled + "/attendance"), coach.token(), marks(first, "PRESENT", second, "ABSENT"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CANCELLED"));
    }

    @Test
    void theRosterOfAPastSessionOnlyHasEnrollmentsStartedByItsMonth() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        int athlete = activeAthlete(guardian.token(), YearMonth.from(today));
        int pastSession = pastSession(today.minusMonths(1).withDayOfMonth(1));
        String url = SESSIONS + "/" + pastSession + "/attendance";

        send(get(url), coach.token(), null).andExpect(jsonPath("$.records.length()").value(0));
        send(put(url), coach.token(), "{\"records\":[{\"athleteId\":" + athlete + ",\"status\":\"PRESENT\"}]}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ATHLETE_NOT_IN_ROSTER"));
        send(get(SESSIONS + "/" + pastSession), guardian.token(), null).andExpect(status().isForbidden());
    }

    @Test
    void eachRoleSeesTheSessionsInItsScope() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        int athlete = activeAthlete(guardian.token(), YearMonth.from(today));
        int sessionId = createSession(groupId, today, "09:00", "10:00");
        String week = "&from=" + today + "&to=" + today.plusDays(6);

        send(get(SESSIONS + "?groupId=" + groupId), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId))
                .andExpect(jsonPath("$[0].athletes").doesNotExist());
        send(get(SESSIONS + "?status=SCHEDULED" + week), coach.token(), null)
                .andExpect(jsonPath("$.length()").value(1));
        send(get(SESSIONS + "?groupId=" + groupId + week), auth.newCoachToken(), null)
                .andExpect(jsonPath("$.length()").value(0));
        send(get(SESSIONS + "?athleteId=" + athlete + week), adminToken, null)
                .andExpect(jsonPath("$.length()").value(1));
        send(get(SESSIONS + "?groupId=" + groupId + week), guardian.token(), null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].athletes[0].id").value(athlete))
                .andExpect(jsonPath("$[0].athletes[0].firstNames").value("Sofía"));
        send(get(SESSIONS + "?groupId=" + groupId + week), auth.newGuardianToken(), null)
                .andExpect(jsonPath("$.length()").value(0));
        send(get(SESSIONS + "?from=" + today + "&to=" + today.plusDays(62)), adminToken, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

        send(get(SESSIONS + "/" + sessionId), guardian.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.athletes[0].id").value(athlete));
        send(get(SESSIONS + "/" + sessionId), auth.newGuardianToken(), null).andExpect(status().isForbidden());
        send(get(SESSIONS + "/" + sessionId), auth.newCoachToken(), null).andExpect(status().isForbidden());
        send(get(SESSIONS + "/" + sessionId), coach.token(), null).andExpect(status().isOk());
        send(get(SESSIONS + "/999999999"), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void futureSessionsBlockDeactivationAndMoveToTheNewCoach() throws Exception {
        int past = pastSession(today.minusDays(1));
        int future = createSession(groupId, today.plusDays(1), "10:00", "11:00");

        send(patch("/api/v1/groups/" + groupId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GROUP_HAS_ACTIVITY"));

        TestCoach newCoach = auth.newCoach();
        send(put("/api/v1/groups/" + groupId + "/coach"), adminToken, "{\"coachId\":" + newCoach.coachId() + "}")
                .andExpect(status().isOk());
        send(get(SESSIONS + "/" + future), adminToken, null).andExpect(jsonPath("$.coach.id").value(newCoach.coachId()));
        send(get(SESSIONS + "/" + past), adminToken, null).andExpect(jsonPath("$.coach.id").value(coach.coachId()));
        send(get(SESSIONS + "/" + future), coach.token(), null).andExpect(status().isForbidden());

        send(post(SESSIONS + "/" + future + "/cancel"), newCoach.token(), "{\"reason\":\"Sin cancha\"}")
                .andExpect(status().isOk());
        send(patch("/api/v1/groups/" + groupId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
    }

    @Test
    void simultaneousOperationsDoNotDuplicateSessionsOrMarks() throws Exception {
        String body = sessionJson(groupId, today.plusDays(1), "18:00", "19:00", null);
        List<Integer> statuses = ConcurrentRequests.run(
                () -> statusOf(send(post(SESSIONS), adminToken, body)),
                () -> statusOf(send(post(SESSIONS), coach.token(), body)));
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);

        TestGuardian guardian = auth.newGuardian();
        int first = activeAthlete(guardian.token(), YearMonth.from(today));
        int second = activeAthlete(guardian.token(), YearMonth.from(today));
        int sessionId = createSession(groupId, today, "04:00", "05:00");
        String url = SESSIONS + "/" + sessionId + "/attendance";
        List<Integer> saves = ConcurrentRequests.run(
                () -> statusOf(send(put(url), coach.token(), marks(first, "PRESENT", second, "ABSENT"))),
                () -> statusOf(send(put(url), coach.token(), marks(first, "ABSENT", second, "PRESENT"))));
        assertThat(saves).containsOnly(200);
        send(get(RECORDS + "?groupId=" + groupId), adminToken, null).andExpect(jsonPath("$.totalElements").value(2));
    }

    /** Matrícula {@code ACTIVE} de un deportista nuevo del acudiente, iniciada en el mes indicado. */
    private int activeAthlete(String guardianToken, YearMonth period) throws Exception {
        int athleteId = academy.newAthlete(guardianToken);
        String enrollment = academy.requestEnrollment(guardianToken, athleteId, groupId, period.toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int enrollmentId = JsonPath.read(enrollment, "$.id");
        send(post("/api/v1/enrollments/" + enrollmentId + "/confirm"), adminToken, null).andExpect(status().isOk());
        return athleteId;
    }

    /** Sesión con fecha pasada, que la API no permite crear, guardada directamente en la base. */
    private int pastSession(LocalDate date) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        return sessionRepository.save(new TrainingSession(group, date, LocalTime.of(8, 0), LocalTime.of(9, 0))).getId();
    }

    private int createSession(int group, LocalDate date, String start, String end) throws Exception {
        String response = send(post(SESSIONS), adminToken, sessionJson(group, date, start, end, null))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static String sessionJson(int group, LocalDate date, String start, String end, String extra) {
        return "{\"groupId\":%d,\"date\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"%s}"
                .formatted(group, date, start, end, extra == null ? "" : "," + extra);
    }

    private static String updateJson(LocalDate date, String start, String end) {
        return "{\"date\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}".formatted(date, start, end);
    }

    private static String generateJson(int group, LocalDate from, LocalDate to) {
        return "{\"groupId\":%d,\"dateFrom\":\"%s\",\"dateTo\":\"%s\",\"notify\":true}".formatted(group, from, to);
    }

    private static String marks(int firstId, String firstStatus, int secondId, String secondStatus) {
        return "{\"records\":[{\"athleteId\":%d,\"status\":\"%s\"},{\"athleteId\":%d,\"status\":\"%s\"}]}"
                .formatted(firstId, firstStatus, secondId, secondStatus);
    }

    private static int statusOf(ResultActions result) {
        return result.andReturn().getResponse().getStatus();
    }
}
