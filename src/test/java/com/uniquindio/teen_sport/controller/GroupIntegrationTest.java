package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient.TestCoach;
import com.uniquindio.teen_sport.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU33: grupos (H6–H11) y cierre de {@code COACH_HAS_ACTIVE_GROUPS} (U10) y
 * {@code DISCIPLINE_HAS_ACTIVE_GROUPS} (H5), contra la API real con PostgreSQL.
 */
@IntegrationTest
class GroupIntegrationTest {

    private static final String GROUPS = "/api/v1/groups";

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;
    private AcademyTestClient academy;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
        academy = new AcademyTestClient(mockMvc, adminToken);
    }

    @Test
    void adminCreatesActiveGroupWithoutSchedules() throws Exception {
        int disciplineId = academy.newDiscipline();
        TestCoach coach = auth.newCoach();

        send(post(GROUPS), adminToken, groupJson(disciplineId, coach.coachId(), "Sub-15 A", 12))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/groups/")))
                .andExpect(jsonPath("$.name").value("Sub-15 A"))
                .andExpect(jsonPath("$.discipline.id").value(disciplineId))
                .andExpect(jsonPath("$.coach.id").value(coach.coachId()))
                .andExpect(jsonPath("$.coach.fullName").value("Luis Entrenador"))
                .andExpect(jsonPath("$.maxCapacity").value(12))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.schedules.length()").value(0));
    }

    @Test
    void creationValidatesCapacityDisciplineAndCoach() throws Exception {
        int disciplineId = academy.newDiscipline();
        TestCoach coach = auth.newCoach();

        for (int capacity : new int[]{0, 16}) {
            send(post(GROUPS), adminToken, groupJson(disciplineId, coach.coachId(), "A", capacity))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("INVALID_MAX_CAPACITY"));
        }
        send(post(GROUPS), adminToken, groupJson(999_999_999, coach.coachId(), "A", 10))
                .andExpect(status().isNotFound());
        send(post(GROUPS), adminToken, groupJson(disciplineId, 999_999_999, "A", 10))
                .andExpect(status().isNotFound());
        send(post(GROUPS), adminToken, "{\"name\":\"A\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='disciplineId')]").exists());

        int inactiveDiscipline = academy.newDiscipline();
        send(patch("/api/v1/disciplines/" + inactiveDiscipline + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(post(GROUPS), adminToken, groupJson(inactiveDiscipline, coach.coachId(), "A", 10))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DISCIPLINE_INACTIVE"));

        TestCoach inactiveCoach = auth.newCoach();
        send(patch("/api/v1/users/" + inactiveCoach.userId() + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(post(GROUPS), adminToken, groupJson(disciplineId, inactiveCoach.coachId(), "A", 10))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_COACH"));
    }

    @Test
    void coachOnlySeesOwnGroups() throws Exception {
        int disciplineId = academy.newDiscipline();
        TestCoach owner = auth.newCoach();
        TestCoach other = auth.newCoach();
        int groupId = academy.newGroup(disciplineId, owner.coachId());
        academy.addSchedule(groupId, "MONDAY", "16:00", "18:00").andExpect(status().isCreated());

        send(get(GROUPS), owner.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(groupId))
                .andExpect(jsonPath("$[0].schedules[0].startTime").value("16:00"));
        send(get(GROUPS).param("coachId", String.valueOf(owner.coachId())), other.token(), null)
                .andExpect(jsonPath("$[*].id", not(hasItem(groupId))));
        send(get(GROUPS + "/" + groupId), other.token(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        send(get(GROUPS + "/" + groupId), owner.token(), null).andExpect(status().isOk());
    }

    @Test
    void adminFiltersGroups() throws Exception {
        int disciplineId = academy.newDiscipline();
        TestCoach coach = auth.newCoach();
        int active = academy.newGroup(disciplineId, coach.coachId());
        int inactive = academy.newGroup(disciplineId, coach.coachId());
        send(patch(GROUPS + "/" + inactive + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        send(get(GROUPS).param("disciplineId", String.valueOf(disciplineId)), adminToken, null)
                .andExpect(jsonPath("$.length()").value(2));
        send(get(GROUPS).param("disciplineId", String.valueOf(disciplineId)).param("status", "ACTIVE"), adminToken, null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(active));
        send(get(GROUPS).param("coachId", String.valueOf(coach.coachId())), adminToken, null)
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void adminUpdatesNameAndCapacity() throws Exception {
        int groupId = academy.newGroup(academy.newDiscipline(), auth.newCoach().coachId());

        send(put(GROUPS + "/" + groupId), adminToken, "{\"name\":\"Renombrado\",\"maxCapacity\":8}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renombrado"))
                .andExpect(jsonPath("$.maxCapacity").value(8));
        send(put(GROUPS + "/" + groupId), adminToken, "{\"name\":\"X\",\"maxCapacity\":20}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_MAX_CAPACITY"));
        send(put(GROUPS + "/999999999"), adminToken, "{\"name\":\"X\",\"maxCapacity\":5}")
                .andExpect(status().isNotFound());
    }

    @Test
    void assignCoachRejectsInactiveOrBusyCoachAndAcceptsFreeOne() throws Exception {
        int disciplineId = academy.newDiscipline();
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());
        academy.addSchedule(groupId, "WEDNESDAY", "16:00", "18:00").andExpect(status().isCreated());

        TestCoach busy = auth.newCoach();
        int busyGroup = academy.newGroup(disciplineId, busy.coachId());
        academy.addSchedule(busyGroup, "WEDNESDAY", "17:00", "19:00").andExpect(status().isCreated());
        send(put(GROUPS + "/" + groupId + "/coach"), adminToken, coachJson(busy.coachId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COACH_SCHEDULE_CONFLICT"));

        TestCoach inactive = auth.newCoach();
        send(patch("/api/v1/users/" + inactive.userId() + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(put(GROUPS + "/" + groupId + "/coach"), adminToken, coachJson(inactive.coachId()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_COACH"));

        TestCoach free = auth.newCoach();
        send(put(GROUPS + "/" + groupId + "/coach"), adminToken, coachJson(free.coachId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coach.id").value(free.coachId()));
        send(get(GROUPS + "/" + groupId), free.token(), null).andExpect(status().isOk());
    }

    @Test
    void busyScheduleOfAnInactiveGroupDoesNotBlockTheCoach() throws Exception {
        int disciplineId = academy.newDiscipline();
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());
        academy.addSchedule(groupId, "THURSDAY", "16:00", "18:00").andExpect(status().isCreated());
        TestCoach coach = auth.newCoach();
        int inactiveGroup = academy.newGroup(disciplineId, coach.coachId());
        academy.addSchedule(inactiveGroup, "THURSDAY", "16:00", "18:00").andExpect(status().isCreated());
        send(patch(GROUPS + "/" + inactiveGroup + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());

        send(put(GROUPS + "/" + groupId + "/coach"), adminToken, coachJson(coach.coachId()))
                .andExpect(status().isOk());
    }

    @Test
    void reactivatingGroupIsRejectedIfItsSlotsCrossAnotherActiveGroupOfTheCoach() throws Exception {
        int disciplineId = academy.newDiscipline();
        TestCoach coach = auth.newCoach();
        int first = academy.newGroup(disciplineId, coach.coachId());
        int second = academy.newGroup(disciplineId, coach.coachId());
        academy.addSchedule(first, "MONDAY", "16:00", "18:00").andExpect(status().isCreated());
        send(patch(GROUPS + "/" + first + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        academy.addSchedule(second, "MONDAY", "16:00", "18:00").andExpect(status().isCreated());

        send(patch(GROUPS + "/" + first + "/status"), adminToken, "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COACH_SCHEDULE_CONFLICT"));
        send(get(GROUPS + "/" + first), adminToken, null).andExpect(jsonPath("$.status").value("INACTIVE"));

        send(patch(GROUPS + "/" + second + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(patch(GROUPS + "/" + first + "/status"), adminToken, "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void coachWithActiveGroupsCannotBeDeactivatedUntilTheyAreInactive() throws Exception {
        TestCoach coach = auth.newCoach();
        int groupId = academy.newGroup(academy.newDiscipline(), coach.coachId());

        send(patch("/api/v1/users/" + coach.userId() + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COACH_HAS_ACTIVE_GROUPS"));
        send(patch(GROUPS + "/" + groupId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(patch("/api/v1/users/" + coach.userId() + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
    }

    @Test
    void disciplineWithActiveGroupsCannotBeDeactivatedUntilTheyAreInactive() throws Exception {
        int disciplineId = academy.newDiscipline();
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());

        send(patch("/api/v1/disciplines/" + disciplineId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DISCIPLINE_HAS_ACTIVE_GROUPS"));
        send(patch(GROUPS + "/" + groupId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(patch("/api/v1/disciplines/" + disciplineId + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
    }

    @Test
    void rolesMatrix() throws Exception {
        TestCoach coach = auth.newCoach();
        int groupId = academy.newGroup(academy.newDiscipline(), coach.coachId());
        String guardianToken = auth.newGuardianToken();

        send(get(GROUPS), guardianToken, null).andExpect(status().isForbidden());
        send(get(GROUPS + "/" + groupId), guardianToken, null).andExpect(status().isForbidden());
        for (String token : new String[]{guardianToken, coach.token()}) {
            send(post(GROUPS), token, "{}").andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            send(put(GROUPS + "/" + groupId), token, "{}").andExpect(status().isForbidden());
            send(patch(GROUPS + "/" + groupId + "/status"), token, "{}").andExpect(status().isForbidden());
            send(put(GROUPS + "/" + groupId + "/coach"), token, "{}").andExpect(status().isForbidden());
            send(post(GROUPS + "/" + groupId + "/schedules"), token, "{}").andExpect(status().isForbidden());
        }
        mockMvc.perform(get(GROUPS)).andExpect(status().isUnauthorized());
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static String groupJson(int disciplineId, int coachId, String name, int capacity) {
        return "{\"disciplineId\":%d,\"coachId\":%d,\"name\":\"%s\",\"maxCapacity\":%d}"
                .formatted(disciplineId, coachId, name, capacity);
    }

    private static String coachJson(int coachId) {
        return "{\"coachId\":%d}".formatted(coachId);
    }
}
