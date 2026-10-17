package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.ConcurrentRequests;
import com.uniquindio.teen_sport.support.AuthTestClient.TestGuardian;
import com.uniquindio.teen_sport.support.IntegrationTest;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU35: solicitud y consulta de matrículas (E6–E8) contra la API real con PostgreSQL, incluidos los
 * cupos bajo concurrencia y los cierres de H9, H10 y {@code hasActiveEnrollment} (E3).
 */
@IntegrationTest
class EnrollmentIntegrationTest {

    private static final String ENROLLMENTS = "/api/v1/enrollments";
    private static final YearMonth CURRENT = YearMonth.now(ZoneId.of("America/Bogota"));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private AuthTestClient auth;
    private AcademyTestClient academy;
    private String adminToken;
    private int disciplineId;
    private int coachId;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
        academy = new AcademyTestClient(mockMvc, adminToken);
        disciplineId = academy.newDiscipline();
        coachId = auth.newCoach().coachId();
    }

    @Test
    void guardianRequestsEnrollmentThatStartsPendingAndOverdue() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        int athleteId = academy.newAthlete(guardian.token());
        int groupId = academy.newGroup(disciplineId, coachId);
        academy.addSchedule(groupId, "MONDAY", "16:00", "18:00").andExpect(status().isCreated());

        int enrollmentId = idOf(academy.requestEnrollment(guardian.token(), athleteId, groupId, CURRENT.toString())
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/enrollments/")))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.paymentStatus").value("OVERDUE"))
                .andExpect(jsonPath("$.paymentReported").value(false))
                .andExpect(jsonPath("$.period").value(CURRENT.toString()))
                .andExpect(jsonPath("$.athlete.id").value(athleteId))
                .andExpect(jsonPath("$.athlete.idNumber").doesNotExist())
                .andExpect(jsonPath("$.discipline.id").value(disciplineId))
                .andExpect(jsonPath("$.schedules[0].startTime").value("16:00"))
                .andExpect(jsonPath("$.confirmedAt").isEmpty()));

        send(get(ENROLLMENTS + "/" + enrollmentId), guardian.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.id").value(groupId));
        send(get("/api/v1/athletes/" + athleteId), guardian.token(), null)
                .andExpect(jsonPath("$.hasActiveEnrollment").value(true));
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        int athleteId = academy.newAthlete(guardian.token());
        int groupId = academy.newGroup(disciplineId, coachId);
        int inactiveGroup = academy.newGroup(disciplineId, coachId);
        send(patch("/api/v1/groups/" + inactiveGroup + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());

        academy.requestEnrollment(guardian.token(), athleteId, groupId, CURRENT.minusMonths(1).toString())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_IN_PAST"));
        academy.requestEnrollment(guardian.token(), athleteId, inactiveGroup, CURRENT.toString())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("GROUP_INACTIVE"));
        academy.requestEnrollment(guardian.token(), athleteId, 999_999_999, CURRENT.toString())
                .andExpect(status().isNotFound());
        academy.requestEnrollment(guardian.token(), 999_999_999, groupId, CURRENT.toString())
                .andExpect(status().isNotFound());
        academy.requestEnrollment(guardian.token(), athleteId, groupId, "2026-13")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        send(post(ENROLLMENTS), guardian.token(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='period')]").exists());
    }

    @Test
    void onlyOneEnrollmentInForcePerDisciplineButOtherDisciplinesAreAllowed() throws Exception {
        String token = auth.newGuardianToken();
        int athleteId = academy.newAthlete(token);
        int first = academy.newGroup(disciplineId, coachId);
        int second = academy.newGroup(disciplineId, coachId);
        int otherDiscipline = academy.newGroup(academy.newDiscipline(), coachId);

        academy.requestEnrollment(token, athleteId, first, CURRENT.toString()).andExpect(status().isCreated());
        academy.requestEnrollment(token, athleteId, second, CURRENT.plusMonths(3).toString())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ENROLLMENT"));
        academy.requestEnrollment(token, athleteId, otherDiscipline, CURRENT.toString()).andExpect(status().isCreated());
    }

    @Test
    void guardiansOnlyEnrollAndSeeTheirOwnAthletes() throws Exception {
        TestGuardian owner = auth.newGuardian();
        TestGuardian other = auth.newGuardian();
        int athleteId = academy.newAthlete(owner.token());
        int groupId = academy.newGroup(disciplineId, coachId);

        academy.requestEnrollment(other.token(), athleteId, groupId, CURRENT.toString())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        int enrollmentId = idOf(academy.requestEnrollment(adminToken, athleteId, groupId, CURRENT.toString()));

        send(get(ENROLLMENTS + "/" + enrollmentId), other.token(), null).andExpect(status().isForbidden());
        send(get(ENROLLMENTS + "?guardianId=" + owner.guardianId()), other.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        send(get(ENROLLMENTS), owner.token(), null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(enrollmentId));
        send(get(ENROLLMENTS + "?guardianId=" + owner.guardianId()), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(1));
        send(get(ENROLLMENTS + "/999999999"), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void coachesCannotUseEnrollmentsOrAvailability() throws Exception {
        String coachToken = auth.newCoachToken();

        send(get(ENROLLMENTS), coachToken, null).andExpect(status().isForbidden());
        send(post(ENROLLMENTS), coachToken, "{}").andExpect(status().isForbidden());
        send(get("/api/v1/availability?period=" + CURRENT), coachToken, null).andExpect(status().isForbidden());
    }

    @Test
    void listFiltersByGroupPaymentAndMonthInForce() throws Exception {
        String token = auth.newGuardianToken();
        int groupId = academy.newGroup(disciplineId, coachId);
        int now = idOf(academy.requestEnrollment(token, academy.newAthlete(token), groupId, CURRENT.toString()));
        int later = idOf(academy.requestEnrollment(token, academy.newAthlete(token), groupId,
                CURRENT.plusMonths(2).toString()));
        String byGroup = ENROLLMENTS + "?groupId=" + groupId;

        send(get(byGroup), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(later));
        send(get(byGroup + "&period=" + CURRENT), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(now));
        send(get(byGroup + "&period=" + CURRENT.plusMonths(2)), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(2));
        send(get(byGroup + "&paymentStatus=OVERDUE&paymentReported=false&status=PENDING_REVIEW"), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(2));
        send(get(byGroup + "&paymentStatus=UP_TO_DATE"), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(0));
        send(get(byGroup + "&disciplineId=" + disciplineId + "&size=1"), adminToken, null)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        send(get(byGroup + "&status=UNKNOWN"), adminToken, null).andExpect(status().isBadRequest());
    }

    @Test
    void laterStartingEnrollmentsAlsoTakeTheSpot() throws Exception {
        String token = auth.newGuardianToken();
        int groupId = academy.newGroup(disciplineId, coachId, 1);
        academy.requestEnrollment(token, academy.newAthlete(token), groupId, CURRENT.plusMonths(2).toString())
                .andExpect(status().isCreated());

        academy.requestEnrollment(token, academy.newAthlete(token), groupId, CURRENT.toString())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_AVAILABLE_SPOTS"));
        send(get("/api/v1/availability?period=" + CURRENT + "&disciplineId=" + disciplineId), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].occupiedSpots").value(0))
                .andExpect(jsonPath("$[0].availableSpots").value(0));
    }

    @Test
    void concurrentRequestsForTheLastSpotSaveOnlyOne() throws Exception {
        String token = auth.newGuardianToken();
        int groupId = academy.newGroup(disciplineId, coachId, 1);
        int firstAthlete = academy.newAthlete(token);
        int secondAthlete = academy.newAthlete(token);

        List<Integer> statuses = ConcurrentRequests.run(
                () -> statusOf(academy.requestEnrollment(token, firstAthlete, groupId, CURRENT.toString())),
                () -> statusOf(academy.requestEnrollment(token, secondAthlete, groupId, CURRENT.toString())));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(enrollmentRepository.countByGroupIdAndStatusIn(groupId, EnrollmentStatus.IN_FORCE)).isEqualTo(1);
    }

    @Test
    void concurrentRequestsOfTheSameAthleteInOneDisciplineSaveOnlyOne() throws Exception {
        String token = auth.newGuardianToken();
        int athleteId = academy.newAthlete(token);
        int first = academy.newGroup(disciplineId, coachId);
        int second = academy.newGroup(disciplineId, coachId);

        List<Integer> statuses = ConcurrentRequests.run(
                () -> statusOf(academy.requestEnrollment(token, athleteId, first, CURRENT.toString())),
                () -> statusOf(academy.requestEnrollment(token, athleteId, second, CURRENT.toString())));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(enrollmentRepository.existsByAthleteIdAndStatusIn(athleteId, EnrollmentStatus.IN_FORCE)).isTrue();
        send(get(ENROLLMENTS + "?athleteId=" + athleteId), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void groupCapacityAndStatusRespectEnrollmentsInForce() throws Exception {
        String token = auth.newGuardianToken();
        int groupId = academy.newGroup(disciplineId, coachId);
        academy.requestEnrollment(token, academy.newAthlete(token), groupId, CURRENT.toString())
                .andExpect(status().isCreated());
        academy.requestEnrollment(token, academy.newAthlete(token), groupId, CURRENT.plusMonths(1).toString())
                .andExpect(status().isCreated());

        send(put("/api/v1/groups/" + groupId), adminToken, "{\"name\":\"Grupo\",\"maxCapacity\":1}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_BELOW_OCCUPIED"));
        send(put("/api/v1/groups/" + groupId), adminToken, "{\"name\":\"Grupo\",\"maxCapacity\":2}")
                .andExpect(status().isOk());
        send(patch("/api/v1/groups/" + groupId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GROUP_HAS_ACTIVITY"));
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static int statusOf(ResultActions result) {
        return result.andReturn().getResponse().getStatus();
    }

    private static int idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }
}
