package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient.TestCoach;
import com.uniquindio.teen_sport.support.AuthTestClient.TestGuardian;
import com.uniquindio.teen_sport.support.IntegrationTest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R1–R5 contra la API real, con PostgreSQL.
 *
 * <p>Como la base se comparte entre clases, R1 usa un periodo lejano que ninguna otra prueba usa, y
 * R2–R4 se filtran por un grupo propio. El estado de pago de R1 incluye matrículas de otros periodos,
 * así que se compara la diferencia antes y después de crear los datos. R5 cuenta todas las cuentas de
 * la base, por lo que también se compara por diferencia.</p>
 */
@IntegrationTest
class ReportIntegrationTest {

    private static final String REPORTS = "/api/v1/reports";
    private static final String ENROLLMENTS = "/api/v1/enrollments/";
    private static final String SESSIONS = "/api/v1/sessions";
    private static final ZoneId ACADEMY_ZONE = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;
    private AcademyTestClient academy;
    private String adminToken;
    private TestCoach coach;
    private TestGuardian guardian;
    private int disciplineId;
    private int groupId;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
        academy = new AcademyTestClient(mockMvc, adminToken);
        coach = auth.newCoach();
        guardian = auth.newGuardian();
        disciplineId = academy.newDiscipline();
        groupId = academy.newGroup(disciplineId, coach.coachId());
        today = LocalDate.now(ACADEMY_ZONE);
    }

    @Test
    void enrollmentReportCountsTheEnrollmentsStartingInThePeriod() throws Exception {
        YearMonth period = YearMonth.from(today).plusYears(70);
        String url = REPORTS + "/enrollments?period=" + period;
        int overdueBefore = JsonPath.read(send(get(url), adminToken, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.payments.overdue");

        int confirmed = enrollment(period);
        int rejected = enrollment(period);
        int cancelled = enrollment(period);
        enrollment(period);
        send(post(ENROLLMENTS + confirmed + "/confirm"), adminToken, null).andExpect(status().isOk());
        send(post(ENROLLMENTS + rejected + "/reject"), adminToken, "{\"reason\":\"Documentación incompleta\"}")
                .andExpect(status().isOk());
        send(post(ENROLLMENTS + cancelled + "/cancel"), guardian.token(), null).andExpect(status().isOk());

        send(get(url), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value(period.toString()))
                .andExpect(jsonPath("$.totalRequests").value(4))
                .andExpect(jsonPath("$.byStatus.PENDING_REVIEW").value(1))
                .andExpect(jsonPath("$.byStatus.ACTIVE").value(1))
                .andExpect(jsonPath("$.byStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.byStatus.CANCELLED").value(1))
                .andExpect(jsonPath("$.completionRate").value(25.0))
                .andExpect(jsonPath("$.abandonmentRate").value(25.0))
                .andExpect(jsonPath("$.averageConfirmationHours").isNumber())
                .andExpect(jsonPath("$.byDiscipline", hasSize(1)))
                .andExpect(jsonPath("$.byDiscipline[0].disciplineId").value(disciplineId))
                .andExpect(jsonPath("$.byDiscipline[0].requests").value(4))
                .andExpect(jsonPath("$.byDiscipline[0].active").value(1))
                .andExpect(jsonPath("$.payments.overdue").value(overdueBefore + 1));
    }

    @Test
    void userReportCountsNewAccountsInactiveOnesAndGuardiansWithAthletes() throws Exception {
        String url = REPORTS + "/users?period=" + YearMonth.from(today);
        String before = send(get(url), adminToken, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        academy.newAthlete(guardian.token());
        auth.newGuardian();
        TestCoach inactive = auth.newCoach();
        send(patch("/api/v1/users/" + inactive.userId() + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());

        String after = send(get(url), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value(YearMonth.from(today).toString()))
                .andExpect(jsonPath("$.byRole[*].role").value(contains("ADMIN", "COACH", "GUARDIAN")))
                .andReturn().getResponse().getContentAsString();

        assertThat(difference(before, after, "$.registeredInPeriod")).isEqualTo(2);
        assertThat(difference(before, after, "$.byRole[1].registeredInPeriod")).isEqualTo(1);
        assertThat(difference(before, after, "$.byRole[2].registeredInPeriod")).isEqualTo(1);
        assertThat(difference(before, after, "$.byRole[1].inactive")).isEqualTo(1);
        assertThat(difference(before, after, "$.byRole[1].active")).isZero();
        assertThat(difference(before, after, "$.inactiveAccounts")).isEqualTo(1);
        assertThat(difference(before, after, "$.byRole[2].active")).isEqualTo(1);
        assertThat(difference(before, after, "$.guardiansWithAthletes")).isEqualTo(1);
        int withAthletes = JsonPath.read(after, "$.guardiansWithAthletes");
        int activeGuardians = JsonPath.read(after, "$.byRole[2].active");
        double rate = ((Number) JsonPath.read(after, "$.guardianLinkRate")).doubleValue();
        assertThat(rate).isCloseTo(100.0 * withAthletes / activeGuardians, within(0.05));
    }

    @Test
    void userReportOfAPeriodWithoutAccountsHasZeroRegistrations() throws Exception {
        send(get(REPORTS + "/users?period=" + YearMonth.from(today).plusYears(70)), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredInPeriod").value(0))
                .andExpect(jsonPath("$.byRole", hasSize(3)))
                .andExpect(jsonPath("$.byRole[*].registeredInPeriod").value(contains(0, 0, 0)));
    }

    @Test
    void userReportIsOnlyForAdminsAndRequiresAValidPeriod() throws Exception {
        String url = REPORTS + "/users?period=" + YearMonth.from(today);
        send(get(url), coach.token(), null).andExpect(status().isForbidden());
        send(get(url), guardian.token(), null).andExpect(status().isForbidden());
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        send(get(REPORTS + "/users"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        send(get(REPORTS + "/users?period=2026-13"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void enrollmentReportIsOnlyForAdminsAndRequiresAValidPeriod() throws Exception {
        String url = REPORTS + "/enrollments?period=" + YearMonth.from(today);
        send(get(url), coach.token(), null).andExpect(status().isForbidden());
        send(get(url), guardian.token(), null).andExpect(status().isForbidden());
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        send(get(REPORTS + "/enrollments"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        send(get(REPORTS + "/enrollments?period=2026-13"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void attendanceReportsReflectTheRecordedMarksWithinEachRolesScope() throws Exception {
        int present = activeAthlete();
        int absent = activeAthlete();
        recordAttendance(createSession(today, "05:00", "06:00"), present, "PRESENT", absent, "ABSENT");
        recordAttendance(createSession(today, "06:00", "07:00"), present, "ABSENT", absent, "ABSENT");
        recordAttendance(createSession(today, "07:00", "08:00"), present, "ABSENT", absent, "ABSENT");
        createSession(today, "08:00", "09:00");
        int cancelled = createSession(today, "09:00", "10:00");
        send(post(SESSIONS + "/" + cancelled + "/cancel"), adminToken, "{\"reason\":\"Lluvia\"}").andExpect(status().isOk());
        createSession(today.plusDays(1), "05:00", "06:00");

        String groupReport = REPORTS + "/attendance?groupId=" + groupId;
        for (String token : new String[] {adminToken, coach.token()}) {
            send(get(groupReport), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduledSessions").value(4))
                    .andExpect(jsonPath("$.completedSessions").value(3))
                    .andExpect(jsonPath("$.recordingCoverage").value(75.0))
                    .andExpect(jsonPath("$.attendanceRate").value(16.7))
                    .andExpect(jsonPath("$.byGroup", hasSize(1)))
                    .andExpect(jsonPath("$.byGroup[0].groupId").value(groupId))
                    .andExpect(jsonPath("$.byGroup[0].discipline").isString())
                    .andExpect(jsonPath("$.byGroup[0].attendanceRate").value(16.7));
        }
        send(get(groupReport), auth.newCoachToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledSessions").value(0))
                .andExpect(jsonPath("$.attendanceRate").doesNotExist())
                .andExpect(jsonPath("$.byGroup", hasSize(0)));
        send(get(REPORTS + "/attendance?disciplineId=" + disciplineId), adminToken, null)
                .andExpect(jsonPath("$.scheduledSessions").value(4));

        String athleteReport = REPORTS + "/attendance/athletes/" + present;
        for (String token : new String[] {adminToken, coach.token(), guardian.token()}) {
            send(get(athleteReport), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.athleteId").value(present))
                    .andExpect(jsonPath("$.recordedSessions").value(3))
                    .andExpect(jsonPath("$.present").value(1))
                    .andExpect(jsonPath("$.absent").value(2))
                    .andExpect(jsonPath("$.attendanceRate").value(33.3))
                    .andExpect(jsonPath("$.byGroup[0].groupId").value(groupId));
        }
        send(get(athleteReport + "?dateFrom=" + today.plusDays(1)), guardian.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedSessions").value(0))
                .andExpect(jsonPath("$.attendanceRate").doesNotExist());

        String absences = REPORTS + "/attendance/recurrent-absences?groupId=" + groupId;
        send(get(absences + "&minConsecutive=2"), coach.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].athlete.id").value(absent))
                .andExpect(jsonPath("$[0].group.id").value(groupId))
                .andExpect(jsonPath("$[0].consecutiveAbsences").value(3))
                .andExpect(jsonPath("$[0].lastAttendance").doesNotExist())
                .andExpect(jsonPath("$[1].athlete.id").value(present))
                .andExpect(jsonPath("$[1].consecutiveAbsences").value(2))
                .andExpect(jsonPath("$[1].lastAttendance").value(today.toString()));
        send(get(absences), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].athlete.id").value(absent));
        send(get(absences + "&dateFrom=" + today.plusDays(1)), adminToken, null)
                .andExpect(jsonPath("$", hasSize(0)));
        send(get(absences), auth.newCoachToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void attendanceReportsDenyForeignResourcesAndValidateTheirParameters() throws Exception {
        int athlete = activeAthlete();
        String athleteReport = REPORTS + "/attendance/athletes/" + athlete;

        send(get(athleteReport), auth.newGuardianToken(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        send(get(athleteReport), auth.newCoachToken(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        send(get(REPORTS + "/attendance/athletes/999999999"), adminToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        send(get(athleteReport + "?dateFrom=" + today + "&dateTo=" + today.minusDays(1)), adminToken, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

        send(get(REPORTS + "/attendance"), guardian.token(), null).andExpect(status().isForbidden());
        send(get(REPORTS + "/attendance?dateFrom=" + today.minusDays(366) + "&dateTo=" + today), adminToken, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
        send(get(REPORTS + "/attendance?dateFrom=ayer"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        send(get(REPORTS + "/attendance/recurrent-absences"), guardian.token(), null)
                .andExpect(status().isForbidden());
        send(get(REPORTS + "/attendance/recurrent-absences?minConsecutive=0"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get(REPORTS + "/attendance/recurrent-absences")).andExpect(status().isUnauthorized());
    }

    /** Matrícula pendiente de un deportista nuevo del acudiente, en el grupo de la prueba. */
    private int enrollment(YearMonth period) throws Exception {
        int athleteId = academy.newAthlete(guardian.token());
        String body = academy.requestEnrollment(guardian.token(), athleteId, groupId, period.toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Deportista nuevo del acudiente con matrícula {@code ACTIVE} desde el mes actual. */
    private int activeAthlete() throws Exception {
        int athleteId = academy.newAthlete(guardian.token());
        String body = academy.requestEnrollment(guardian.token(), athleteId, groupId, YearMonth.from(today).toString())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int enrollmentId = JsonPath.read(body, "$.id");
        send(post(ENROLLMENTS + enrollmentId + "/confirm"), adminToken, null).andExpect(status().isOk());
        return athleteId;
    }

    private int createSession(LocalDate date, String start, String end) throws Exception {
        String body = send(post(SESSIONS), adminToken,
                "{\"groupId\":%d,\"date\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}"
                        .formatted(groupId, date, start, end))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void recordAttendance(int sessionId, int firstId, String firstStatus, int secondId, String secondStatus)
            throws Exception {
        send(put(SESSIONS + "/" + sessionId + "/attendance"), coach.token(),
                "{\"records\":[{\"athleteId\":%d,\"status\":\"%s\"},{\"athleteId\":%d,\"status\":\"%s\"}]}"
                        .formatted(firstId, firstStatus, secondId, secondStatus))
                .andExpect(status().isOk());
    }

    private static int difference(String before, String after, String path) {
        return JsonPath.<Integer>read(after, path) - JsonPath.<Integer>read(before, path);
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }
}
