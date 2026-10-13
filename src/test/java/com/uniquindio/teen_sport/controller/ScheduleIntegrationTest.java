package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient.TestCoach;
import com.uniquindio.teen_sport.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.uniquindio.teen_sport.support.AcademyTestClient.scheduleJson;
import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU33: franjas semanales (H12–H14) contra la API real con PostgreSQL, incluidos los cruces entre
 * grupos del mismo entrenador y dos solicitudes simultáneas.
 */
@IntegrationTest
class ScheduleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ScheduleRepository scheduleRepository;

    private AuthTestClient auth;
    private AcademyTestClient academy;
    private String adminToken;
    private int disciplineId;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
        academy = new AcademyTestClient(mockMvc, adminToken);
        disciplineId = academy.newDiscipline();
    }

    @Test
    void createsScheduleWithLocationAndHoursAsHHmm() throws Exception {
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());

        academy.addSchedule(groupId, "MONDAY", "16:30", "18:00")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/schedules/")))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.startTime").value("16:30"))
                .andExpect(jsonPath("$.endTime").value("18:00"));
    }

    @Test
    void rejectsInvalidRangeAndMalformedData() throws Exception {
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());

        academy.addSchedule(groupId, "MONDAY", "18:00", "16:00")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
        academy.addSchedule(groupId, "MONDAY", "25:00", "26:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        academy.addSchedule(groupId, "LUNES", "16:00", "17:00").andExpect(status().isBadRequest());
        send(post("/api/v1/groups/" + groupId + "/schedules"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='dayOfWeek')]").exists());
        academy.addSchedule(999_999_999, "MONDAY", "16:00", "17:00").andExpect(status().isNotFound());
    }

    @Test
    void rejectsOverlapWithinTheGroupButAllowsContiguousSlots() throws Exception {
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());
        academy.addSchedule(groupId, "TUESDAY", "16:00", "18:00").andExpect(status().isCreated());

        academy.addSchedule(groupId, "TUESDAY", "17:00", "19:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
        academy.addSchedule(groupId, "TUESDAY", "18:00", "19:00").andExpect(status().isCreated());
        academy.addSchedule(groupId, "WEDNESDAY", "16:00", "18:00").andExpect(status().isCreated());
    }

    @Test
    void rejectsOverlapWithAnotherActiveGroupOfTheSameCoachOnly() throws Exception {
        TestCoach coach = auth.newCoach();
        int first = academy.newGroup(disciplineId, coach.coachId());
        int second = academy.newGroup(disciplineId, coach.coachId());
        int otherCoachGroup = academy.newGroup(disciplineId, auth.newCoach().coachId());
        academy.addSchedule(first, "FRIDAY", "16:00", "18:00").andExpect(status().isCreated());

        academy.addSchedule(second, "FRIDAY", "17:00", "19:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
        academy.addSchedule(otherCoachGroup, "FRIDAY", "17:00", "19:00").andExpect(status().isCreated());

        send(patch("/api/v1/groups/" + first + "/status"), "{\"status\":\"INACTIVE\"}").andExpect(status().isOk());
        academy.addSchedule(second, "FRIDAY", "17:00", "19:00").andExpect(status().isCreated());
    }

    @Test
    void schedulesCanBeManagedInAnInactiveGroup() throws Exception {
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());
        send(patch("/api/v1/groups/" + groupId + "/status"), "{\"status\":\"INACTIVE\"}").andExpect(status().isOk());

        academy.addSchedule(groupId, "SATURDAY", "08:00", "10:00").andExpect(status().isCreated());
    }

    @Test
    void updateExcludesItselfAndValidatesLikeCreation() throws Exception {
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());
        int scheduleId = idOf(academy.addSchedule(groupId, "MONDAY", "16:00", "18:00"));
        academy.addSchedule(groupId, "MONDAY", "19:00", "20:00").andExpect(status().isCreated());

        send(put("/api/v1/schedules/" + scheduleId), scheduleJson("MONDAY", "16:30", "18:30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("16:30"));
        send(put("/api/v1/schedules/" + scheduleId), scheduleJson("MONDAY", "18:00", "19:30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
        send(put("/api/v1/schedules/" + scheduleId), scheduleJson("MONDAY", "17:00", "17:00"))
                .andExpect(status().isUnprocessableContent());
        send(put("/api/v1/schedules/999999999"), scheduleJson("MONDAY", "08:00", "09:00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesScheduleAndFreesTheSlot() throws Exception {
        int groupId = academy.newGroup(disciplineId, auth.newCoach().coachId());
        int scheduleId = idOf(academy.addSchedule(groupId, "SUNDAY", "09:00", "11:00"));

        send(delete("/api/v1/schedules/" + scheduleId), null).andExpect(status().isNoContent());
        send(delete("/api/v1/schedules/" + scheduleId), null).andExpect(status().isNotFound());
        academy.addSchedule(groupId, "SUNDAY", "09:00", "11:00").andExpect(status().isCreated());
        send(get("/api/v1/groups/" + groupId), null).andExpect(jsonPath("$.schedules.length()").value(1));
    }

    @Test
    void onlyAdminsManageSchedules() throws Exception {
        TestCoach coach = auth.newCoach();
        int scheduleId = idOf(academy.addSchedule(academy.newGroup(disciplineId, coach.coachId()),
                "MONDAY", "06:00", "07:00"));

        for (String token : new String[]{coach.token(), auth.newGuardianToken()}) {
            mockMvc.perform(put("/api/v1/schedules/" + scheduleId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/schedules/" + scheduleId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void concurrentOverlappingSlotsForTheSameCoachSaveOnlyOne() throws Exception {
        TestCoach coach = auth.newCoach();
        int first = academy.newGroup(disciplineId, coach.coachId());
        int second = academy.newGroup(disciplineId, coach.coachId());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int groupId : new int[]{first, second}) {
                Callable<Integer> request = () -> {
                    start.await();
                    return academy.addSchedule(groupId, "SATURDAY", "14:00", "16:00")
                            .andReturn().getResponse().getStatus();
                };
                results.add(executor.submit(request));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            assertThat(scheduleRepository.findByGroupIds(List.of(first, second))).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(adminToken));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static int idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }
}
