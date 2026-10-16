package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.repository.AthleteRepository;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient.TestGuardian;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 * HU34: deportistas (E1–E4) contra la API real, con PostgreSQL. Las fechas se calculan con la zona
 * de la academia, igual que la aplicación.
 */
@IntegrationTest
class AthleteIntegrationTest {

    private static final String ATHLETES = "/api/v1/athletes";
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Bogota"));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private GuardianRepository guardianRepository;

    private AuthTestClient auth;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
    }

    @Test
    void guardianRegistersOwnAthleteEvenIfAnotherGuardianIdIsSent() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        TestGuardian other = auth.newGuardian();

        send(post(ATHLETES), guardian.token(), athleteJson(other.guardianId(), TestAccounts.uniqueIdNumber(), 14))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/athletes/")))
                .andExpect(jsonPath("$.guardianId").value(guardian.guardianId()))
                .andExpect(jsonPath("$.age").value(14))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.hasActiveEnrollment").value(false));
    }

    @Test
    void ageMustBeBetweenTwelveAndSeventeen() throws Exception {
        String token = auth.newGuardianToken();

        for (int age : new int[]{11, 18}) {
            send(post(ATHLETES), token, athleteJson(null, TestAccounts.uniqueIdNumber(), age))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("AGE_OUT_OF_RANGE"));
        }
        send(post(ATHLETES), token, athleteJson(null, TestAccounts.uniqueIdNumber(), 12)).andExpect(status().isCreated());
        send(post(ATHLETES), token, athleteJson(null, TestAccounts.uniqueIdNumber(), 17)).andExpect(status().isCreated());
    }

    @Test
    void duplicateDocumentIsAConflict() throws Exception {
        String idNumber = TestAccounts.uniqueIdNumber();
        send(post(ATHLETES), auth.newGuardianToken(), athleteJson(null, idNumber, 14)).andExpect(status().isCreated());

        send(post(ATHLETES), auth.newGuardianToken(), athleteJson(null, idNumber, 15))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ATHLETE"));
    }

    @Test
    void adminMustIndicateAnExistingActiveGuardian() throws Exception {
        TestGuardian guardian = auth.newGuardian();

        send(post(ATHLETES), adminToken, athleteJson(null, TestAccounts.uniqueIdNumber(), 14))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("guardianId"));
        send(post(ATHLETES), adminToken, athleteJson(999_999_999, TestAccounts.uniqueIdNumber(), 14))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("guardianId"));

        send(post(ATHLETES), adminToken, athleteJson(guardian.guardianId(), TestAccounts.uniqueIdNumber(), 14))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guardianId").value(guardian.guardianId()));

        send(patch("/api/v1/users/" + guardian.userId() + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        send(post(ATHLETES), adminToken, athleteJson(guardian.guardianId(), TestAccounts.uniqueIdNumber(), 14))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("guardianId"));
    }

    @Test
    void guardianOnlyAccessesOwnAthletes() throws Exception {
        TestGuardian owner = auth.newGuardian();
        TestGuardian stranger = auth.newGuardian();
        int athleteId = create(owner.token(), TestAccounts.uniqueIdNumber());

        send(get(ATHLETES + "/" + athleteId), stranger.token(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        send(put(ATHLETES + "/" + athleteId), stranger.token(), updateJson(TestAccounts.uniqueIdNumber(), 14))
                .andExpect(status().isForbidden());
        send(get(ATHLETES).param("guardianId", String.valueOf(owner.guardianId())), stranger.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(athleteId))));

        send(get(ATHLETES + "/" + athleteId), owner.token(), null).andExpect(status().isOk());
        send(get(ATHLETES), owner.token(), null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(athleteId));
        send(get(ATHLETES + "/" + athleteId), adminToken, null).andExpect(status().isOk());
    }

    @Test
    void adminListsFiltersByGuardianAndSearchesByDocument() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        String idNumber = TestAccounts.uniqueIdNumber();
        int athleteId = create(guardian.token(), idNumber);
        create(guardian.token(), TestAccounts.uniqueIdNumber());

        send(get(ATHLETES).param("guardianId", String.valueOf(guardian.guardianId())), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        send(get(ATHLETES).param("q", idNumber), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(athleteId));
        send(get(ATHLETES).param("size", "0"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("size"));
    }

    @Test
    void updateValidatesAgeOnlyWhenBirthDateChangesAndRejectsDuplicates() throws Exception {
        String token = auth.newGuardianToken();
        String idNumber = TestAccounts.uniqueIdNumber();
        int athleteId = create(token, idNumber);
        String takenIdNumber = TestAccounts.uniqueIdNumber();
        create(token, takenIdNumber);

        send(put(ATHLETES + "/" + athleteId), token, updateJson(idNumber, 14))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstNames").value("Sofía Editada"))
                .andExpect(jsonPath("$.gender").isEmpty());
        send(put(ATHLETES + "/" + athleteId), token, updateJson(idNumber, 19))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("AGE_OUT_OF_RANGE"));
        send(put(ATHLETES + "/" + athleteId), token, updateJson(takenIdNumber, 14))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ATHLETE"));
        send(put(ATHLETES + "/999999999"), adminToken, updateJson(TestAccounts.uniqueIdNumber(), 14))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFieldsAreReported() throws Exception {
        send(post(ATHLETES), auth.newGuardianToken(), "{\"firstNames\":\"\",\"idType\":\"TI\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='firstNames')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='birthDate')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='idNumber')]").exists());
    }

    @Test
    void coachesCannotAccessAthletesAndAnonymousIsUnauthenticated() throws Exception {
        String coachToken = auth.newCoachToken();

        send(get(ATHLETES), coachToken, null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        send(post(ATHLETES), coachToken, "{}").andExpect(status().isForbidden());
        send(get(ATHLETES + "/1"), coachToken, null).andExpect(status().isForbidden());
        send(put(ATHLETES + "/1"), coachToken, "{}").andExpect(status().isForbidden());
        mockMvc.perform(get(ATHLETES)).andExpect(status().isUnauthorized());
    }

    @Test
    void documentNumberIsUniqueInTheDatabase() throws Exception {
        TestGuardian guardian = auth.newGuardian();
        String idNumber = TestAccounts.uniqueIdNumber();
        create(guardian.token(), idNumber);
        Athlete copy = new Athlete(guardianRepository.findById(guardian.guardianId()).orElseThrow(), Instant.now());
        copy.setFirstName("Copia");
        copy.setLastName("Prueba");
        copy.setDocumentType("TI");
        copy.setDocumentNumber(idNumber);
        copy.setBirthDate(TODAY.minusYears(14));

        assertThatThrownBy(() -> athleteRepository.saveAndFlush(copy)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private int create(String token, String idNumber) throws Exception {
        String response = send(post(ATHLETES), token, athleteJson(null, idNumber, 14))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static String athleteJson(Integer guardianId, String idNumber, int age) {
        return """
                {"guardianId":%s,"firstNames":"Sofía","lastNames":"Prueba Test","idType":"TI","idNumber":"%s",
                 "birthDate":"%s","gender":"FEMALE"}
                """.formatted(guardianId, idNumber, TODAY.minusYears(age));
    }

    private static String updateJson(String idNumber, int age) {
        return """
                {"firstNames":"Sofía Editada","lastNames":"Prueba Test","idType":"TI","idNumber":"%s","birthDate":"%s"}
                """.formatted(idNumber, TODAY.minusYears(age));
    }
}
