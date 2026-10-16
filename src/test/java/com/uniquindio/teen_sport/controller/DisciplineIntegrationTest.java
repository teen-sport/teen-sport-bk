package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.repository.DisciplineRepository;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.UUID;
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
 * HU32: catálogo de disciplinas (H1–H5) contra la API real, con PostgreSQL.
 */
@IntegrationTest
class DisciplineIntegrationTest {

    private static final String DISCIPLINES = "/api/v1/disciplines";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DisciplineRepository disciplineRepository;

    private AuthTestClient auth;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
    }

    @Test
    void adminCreatesActiveDisciplineWithLocation() throws Exception {
        String name = uniqueName("Fútbol");

        send(post(DISCIPLINES), adminToken, json(name, "Para todas las edades", "120000"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/disciplines/")))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.monthlyFee").value(120000))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void duplicateNameIgnoringCaseIsAConflict() throws Exception {
        String name = uniqueName("Natación");
        create(name);

        send(post(DISCIPLINES), adminToken, json(name.toUpperCase(), null, "90000"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DISCIPLINE"));
    }

    @Test
    void monthlyFeeMustBePositiveWithTwoDecimalsAndFitTheColumn() throws Exception {
        for (String fee : new String[]{"0", "-1", "100.555", "123456789"}) {
            send(post(DISCIPLINES), adminToken, json(uniqueName("Tenis"), null, fee))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value("monthlyFee"));
        }
        send(post(DISCIPLINES), adminToken, json(uniqueName("Tenis"), null, "99999999.99"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminUpdatesDisciplineAndCannotTakeAnotherName() throws Exception {
        int id = create(uniqueName("Baloncesto"));
        String taken = uniqueName("Voleibol");
        create(taken);

        send(put(DISCIPLINES + "/" + id), adminToken, json(uniqueName("Baloncesto Pro"), "Nivel avanzado", "150000.50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nivel avanzado"))
                .andExpect(jsonPath("$.monthlyFee").value(150000.50));
        send(put(DISCIPLINES + "/" + id), adminToken, json(taken, null, "150000"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DISCIPLINE"));
        send(put(DISCIPLINES + "/999999999"), adminToken, json(uniqueName("X"), null, "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveDisciplinesAreHiddenFromNonAdmins() throws Exception {
        int id = create(uniqueName("Patinaje"));
        send(patch(DISCIPLINES + "/" + id + "/status"), adminToken, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        String guardianToken = auth.newGuardianToken();
        mockMvc.perform(get(DISCIPLINES).param("status", "INACTIVE").header(HttpHeaders.AUTHORIZATION, bearer(guardianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(id))))
                .andExpect(jsonPath("$[*].status", not(hasItem("INACTIVE"))));
        mockMvc.perform(get(DISCIPLINES).param("status", "INACTIVE").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$[*].id", hasItem(id)))
                .andExpect(jsonPath("$[*].status", not(hasItem("ACTIVE"))));
        mockMvc.perform(get(DISCIPLINES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(guardianToken)))
                .andExpect(status().isOk());
    }

    @Test
    void everyRoleCanReadButOnlyAdminCanWrite() throws Exception {
        int id = create(uniqueName("Atletismo"));
        String guardianToken = auth.newGuardianToken();
        String coachToken = auth.newCoachToken();

        for (String token : new String[]{guardianToken, coachToken}) {
            mockMvc.perform(get(DISCIPLINES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk());
            mockMvc.perform(get(DISCIPLINES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk());
            // Cuerpos vacíos: el rechazo por rol debe llegar antes que la validación (403, no 400).
            send(post(DISCIPLINES), token, "{}").andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            send(put(DISCIPLINES + "/" + id), token, "{}").andExpect(status().isForbidden());
            send(patch(DISCIPLINES + "/" + id + "/status"), token, "{}").andExpect(status().isForbidden());
        }
        mockMvc.perform(get(DISCIPLINES)).andExpect(status().isUnauthorized());
    }

    @Test
    void statusChangeValidatesInputAndExistence() throws Exception {
        int id = create(uniqueName("Ciclismo"));

        send(patch(DISCIPLINES + "/" + id + "/status"), adminToken, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("status"));
        send(patch(DISCIPLINES + "/999999999/status"), adminToken, "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void nameIsUniqueInTheDatabase() {
        String name = uniqueName("Rugby");
        disciplineRepository.saveAndFlush(new Discipline(name, null, BigDecimal.TEN));

        assertThatThrownBy(() -> disciplineRepository.saveAndFlush(new Discipline(name, null, BigDecimal.TEN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private int create(String name) throws Exception {
        String response = send(post(DISCIPLINES), adminToken, json(name, null, "100000"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        return mockMvc.perform(builder.header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String json(String name, String description, String fee) {
        String descriptionJson = description == null ? "null" : "\"" + description + "\"";
        return "{\"name\":\"%s\",\"description\":%s,\"monthlyFee\":%s}".formatted(name, descriptionJson, fee);
    }

    private static String uniqueName(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
