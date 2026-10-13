package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.IntegrationTest;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU35: disponibilidad de cupos por periodo (E5) contra la API real con PostgreSQL.
 */
@IntegrationTest
class AvailabilityIntegrationTest {

    private static final YearMonth CURRENT = YearMonth.now(ZoneId.of("America/Bogota"));

    @Autowired
    private MockMvc mockMvc;

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
    void showsOpenGroupsOfTheDisciplineWithTheirSpots() throws Exception {
        String guardianToken = auth.newGuardianToken();
        int groupId = academy.newGroup(disciplineId, coachId, 3);
        academy.addSchedule(groupId, "TUESDAY", "15:00", "16:30");
        int closed = academy.newGroup(disciplineId, coachId);
        patchStatus("/api/v1/groups/" + closed + "/status");
        academy.requestEnrollment(guardianToken, academy.newAthlete(guardianToken), groupId, CURRENT.toString())
                .andExpect(status().isCreated());

        availability(guardianToken, "?period=" + CURRENT + "&disciplineId=" + disciplineId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].groupId").value(groupId))
                .andExpect(jsonPath("$[0].discipline.monthlyFee").value(100000))
                .andExpect(jsonPath("$[0].coach.id").value(coachId))
                .andExpect(jsonPath("$[0].schedules[0].endTime").value("16:30"))
                .andExpect(jsonPath("$[0].maxCapacity").value(3))
                .andExpect(jsonPath("$[0].occupiedSpots").value(1))
                .andExpect(jsonPath("$[0].availableSpots").value(2));
    }

    @Test
    void groupsOfAnInactiveDisciplineAreHidden() throws Exception {
        int otherDiscipline = academy.newDiscipline();
        int groupId = academy.newGroup(otherDiscipline, coachId);
        patchStatus("/api/v1/groups/" + groupId + "/status");
        patchStatus("/api/v1/disciplines/" + otherDiscipline + "/status");

        availability(adminToken, "?period=" + CURRENT + "&disciplineId=" + otherDiscipline)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void periodIsRequiredWellFormedAndNotInThePast() throws Exception {
        availability(adminToken, "?period=" + CURRENT.minusMonths(1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_IN_PAST"));
        availability(adminToken, "?period=2026-13")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("period"));
        availability(adminToken, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("period"));
    }

    private ResultActions availability(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/availability" + query).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private void patchStatus(String path) throws Exception {
        mockMvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }
}
