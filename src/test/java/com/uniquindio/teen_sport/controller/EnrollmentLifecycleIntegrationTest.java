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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU37: revisión y ciclo de la matrícula (E12–E15) contra la API real con PostgreSQL: transiciones,
 * liberación de cupos, permisos y cambio de grupo bajo concurrencia.
 */
@IntegrationTest
class EnrollmentLifecycleIntegrationTest {

    private static final String ENROLLMENTS = "/api/v1/enrollments/";
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
    void adminConfirmsWithoutBodyAndTheEnrollmentStaysOverdue() throws Exception {
        String token = auth.newGuardianToken();
        int enrollmentId = enroll(token, academy.newGroup(disciplineId, coachId));

        send(post(ENROLLMENTS + enrollmentId + "/confirm"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.paymentStatus").value("OVERDUE"))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty());
        send(post(ENROLLMENTS + enrollmentId + "/confirm"), adminToken, "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        send(post(ENROLLMENTS + "999999999/confirm"), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void confirmCanMoveToAnotherGroupOfTheSameDiscipline() throws Exception {
        String token = auth.newGuardianToken();
        int enrollmentId = enroll(token, academy.newGroup(disciplineId, coachId));
        int otherGroup = academy.newGroup(disciplineId, coachId);
        int otherDisciplineGroup = academy.newGroup(academy.newDiscipline(), coachId);

        send(post(ENROLLMENTS + enrollmentId + "/confirm"), adminToken, groupJson(otherDisciplineGroup))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("GROUP_OTHER_DISCIPLINE"));
        send(post(ENROLLMENTS + enrollmentId + "/confirm"), adminToken, groupJson(otherGroup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.group.id").value(otherGroup));
    }

    @Test
    void rejectRequiresAReasonAndFreesTheSpot() throws Exception {
        String token = auth.newGuardianToken();
        int groupId = academy.newGroup(disciplineId, coachId, 1);
        int athleteId = academy.newAthlete(token);
        int enrollmentId = enroll(token, athleteId, groupId);
        academy.requestEnrollment(token, academy.newAthlete(token), groupId, CURRENT.toString())
                .andExpect(status().isConflict());

        send(post(ENROLLMENTS + enrollmentId + "/reject"), adminToken, "{\"reason\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("reason"));
        send(post(ENROLLMENTS + enrollmentId + "/reject"), adminToken, "{\"reason\":\"Documentos incompletos\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewNotes").value("Documentos incompletos"));
        send(post(ENROLLMENTS + enrollmentId + "/reject"), adminToken, "{\"reason\":\"Otra vez\"}")
                .andExpect(status().isConflict());

        academy.requestEnrollment(token, academy.newAthlete(token), groupId, CURRENT.toString())
                .andExpect(status().isCreated());
        academy.requestEnrollment(token, athleteId, academy.newGroup(disciplineId, coachId), CURRENT.toString())
                .andExpect(status().isCreated());
    }

    @Test
    void guardianCancelsOnlyOwnEnrollmentsThatAreNotActive() throws Exception {
        TestGuardian owner = auth.newGuardian();
        String otherToken = auth.newGuardianToken();
        int groupId = academy.newGroup(disciplineId, coachId);
        int pending = enroll(owner.token(), groupId);
        int active = enroll(owner.token(), groupId);
        send(post(ENROLLMENTS + active + "/confirm"), adminToken, null).andExpect(status().isOk());

        send(post(ENROLLMENTS + pending + "/cancel"), otherToken, null).andExpect(status().isForbidden());
        send(post(ENROLLMENTS + active + "/cancel"), owner.token(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        send(post(ENROLLMENTS + pending + "/cancel"), owner.token(), "{\"reason\":\"Cambio de horario\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.reviewNotes").value("Cambio de horario"));
        send(post(ENROLLMENTS + pending + "/cancel"), owner.token(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        send(post(ENROLLMENTS + active + "/cancel"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        send(patch("/api/v1/groups/" + groupId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
    }

    @Test
    void changeGroupKeepsThePeriodAndMovesTheSpot() throws Exception {
        String token = auth.newGuardianToken();
        int origin = academy.newGroup(disciplineId, coachId, 1);
        int target = academy.newGroup(disciplineId, coachId, 1);
        String later = CURRENT.plusMonths(1).toString();
        int enrollmentId = idOf(academy.requestEnrollment(token, academy.newAthlete(token), origin, later));

        send(patch(ENROLLMENTS + enrollmentId + "/group"), adminToken, groupJson(target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.id").value(target))
                .andExpect(jsonPath("$.period").value(later))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        send(patch(ENROLLMENTS + enrollmentId + "/group"), adminToken, groupJson(origin)).andExpect(status().isOk());
        enroll(token, target);
        send(patch(ENROLLMENTS + enrollmentId + "/group"), adminToken, groupJson(target))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_AVAILABLE_SPOTS"));
        send(patch(ENROLLMENTS + enrollmentId + "/group"), adminToken, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("groupId"));
    }

    @Test
    void onlyAdminsReviewOrMoveEnrollments() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        int enrollmentId = enroll(guardian.token(), academy.newGroup(disciplineId, coachId));
        String coachToken = auth.newCoachToken();

        for (String token : new String[]{guardian.token(), coachToken}) {
            send(post(ENROLLMENTS + enrollmentId + "/confirm"), token, null).andExpect(status().isForbidden());
            send(post(ENROLLMENTS + enrollmentId + "/reject"), token, "{}").andExpect(status().isForbidden());
            send(patch(ENROLLMENTS + enrollmentId + "/group"), token, "{}").andExpect(status().isForbidden());
        }
        send(post(ENROLLMENTS + enrollmentId + "/cancel"), coachToken, null).andExpect(status().isForbidden());
        send(get(ENROLLMENTS + enrollmentId), guardian.token(), null)
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void changeGroupAndNewRequestCompetingForTheLastSpotSaveOnlyOne() throws Exception {
        String token = auth.newGuardianToken();
        int target = academy.newGroup(disciplineId, coachId, 1);
        int moving = enroll(token, academy.newGroup(disciplineId, coachId));
        int athleteId = academy.newAthlete(token);

        List<Integer> statuses = ConcurrentRequests.run(
                () -> statusOf(send(patch(ENROLLMENTS + moving + "/group"), adminToken, groupJson(target))),
                () -> statusOf(academy.requestEnrollment(token, athleteId, target, CURRENT.toString())));

        // Gana el cambio de grupo (200) o la solicitud nueva (201); la otra recibe 409.
        assertThat(statuses).containsOnlyOnce(409).containsAnyOf(200, 201).hasSize(2);
        assertThat(enrollmentRepository.countByGroupIdAndStatusIn(target, EnrollmentStatus.IN_FORCE)).isEqualTo(1);
    }

    private int enroll(String token, int groupId) throws Exception {
        return enroll(token, academy.newAthlete(token), groupId);
    }

    private int enroll(String token, int athleteId, int groupId) throws Exception {
        return idOf(academy.requestEnrollment(token, athleteId, groupId, CURRENT.toString()));
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static String groupJson(int groupId) {
        return "{\"groupId\":" + groupId + "}";
    }

    private static int statusOf(ResultActions result) {
        return result.andReturn().getResponse().getStatus();
    }

    private static int idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }
}
