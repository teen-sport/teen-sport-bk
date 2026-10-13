package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU30: administración de cuentas (U6–U10) y listados de entrenadores y acudientes (U11, U12).
 */
@IntegrationTest
class UserAdministrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private int adminId;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        String response = loginRequest(TestAccounts.ADMIN_EMAIL, TestAccounts.ADMIN_PASSWORD)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        adminToken = JsonPath.read(response, "$.accessToken");
        adminId = JsonPath.read(response, "$.user.id");
    }

    static Stream<Arguments> adminOnlyEndpoints() {
        String body = "{}";
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/users", null),
                Arguments.of(HttpMethod.GET, "/api/v1/users/1", null),
                Arguments.of(HttpMethod.POST, "/api/v1/users", body),
                Arguments.of(HttpMethod.PUT, "/api/v1/users/1", body),
                Arguments.of(HttpMethod.PATCH, "/api/v1/users/1/status", body),
                Arguments.of(HttpMethod.GET, "/api/v1/coaches", null),
                Arguments.of(HttpMethod.GET, "/api/v1/guardians", null));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("adminOnlyEndpoints")
    void guardianAndCoachAreDeniedAndAnonymousIsUnauthenticated(HttpMethod method, String path, String body)
            throws Exception {
        String guardianToken = registerGuardianAndLogin(TestAccounts.uniqueEmail());
        String coachToken = loginAsNewCoach();

        for (String token : new String[]{guardianToken, coachToken}) {
            perform(method, path, body, token)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
        perform(method, path, body, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void adminCreatesCoachWhoCanLogInAndAppearsInTheCoachList() throws Exception {
        String email = TestAccounts.uniqueEmail();

        String created = asAdmin(post("/api/v1/users"), staffJson("COACH", email, TestAccounts.uniqueIdNumber(),
                TestAccounts.VALID_PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/users/")))
                .andExpect(jsonPath("$.role").value("COACH"))
                .andExpect(jsonPath("$.coach.specialty").value("Fútbol"))
                .andExpect(jsonPath("$.coach.yearsOfExperience").value(5))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        int coachId = JsonPath.read(created, "$.coach.id");

        loginRequest(email, TestAccounts.VALID_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.coachId").value(coachId));
        asAdmin(get("/api/v1/coaches").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(coachId)));
    }

    @Test
    void adminCreatesAnotherAdminWithoutCoachProfile() throws Exception {
        asAdmin(post("/api/v1/users"), staffJson("ADMIN", TestAccounts.uniqueEmail(), TestAccounts.uniqueIdNumber(),
                TestAccounts.VALID_PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.coach").isEmpty());
    }

    @Test
    void guardianRoleCannotBeAssignedThroughStaffCreation() throws Exception {
        asAdmin(post("/api/v1/users"), staffJson("GUARDIAN", TestAccounts.uniqueEmail(),
                TestAccounts.uniqueIdNumber(), TestAccounts.VALID_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("staffRole"));
    }

    @Test
    void staffCreationRejectsDuplicatesAndWeakPasswords() throws Exception {
        asAdmin(post("/api/v1/users"), staffJson("COACH", TestAccounts.ADMIN_EMAIL, TestAccounts.uniqueIdNumber(),
                TestAccounts.VALID_PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        asAdmin(post("/api/v1/users"), staffJson("COACH", TestAccounts.uniqueEmail(), TestAccounts.ADMIN_ID_NUMBER,
                TestAccounts.VALID_PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ID_NUMBER_ALREADY_REGISTERED"));
        asAdmin(post("/api/v1/users"), staffJson("COACH", TestAccounts.uniqueEmail(), TestAccounts.uniqueIdNumber(),
                "debilsinmayuscula1"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void listFiltersByRoleAndTextAndPaginates() throws Exception {
        String marker = "zq" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            asAdmin(post("/api/v1/users"), staffJson("COACH", marker + i + "@teensport.test",
                    TestAccounts.uniqueIdNumber(), TestAccounts.VALID_PASSWORD)).andExpect(status().isCreated());
        }

        asAdmin(get("/api/v1/users").param("q", marker.toUpperCase()).param("role", "COACH").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].coach.specialty").value("Fútbol"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        asAdmin(get("/api/v1/users").param("q", marker).param("role", "GUARDIAN"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchTreatsWildcardsAsLiteralText() throws Exception {
        asAdmin(get("/api/v1/users").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidPaginationIsAValidationError() throws Exception {
        asAdmin(get("/api/v1/users").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("size"));
        asAdmin(get("/api/v1/guardians").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("page"));
    }

    @Test
    void getUserReturnsProfileOrNotFound() throws Exception {
        asAdmin(get("/api/v1/users/" + adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TestAccounts.ADMIN_EMAIL));
        asAdmin(get("/api/v1/users/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void adminUpdatesGuardianIncludingEmailButNeverTheRole() throws Exception {
        String email = TestAccounts.uniqueEmail();
        int userId = registerGuardian(email);
        String newEmail = TestAccounts.uniqueEmail();

        asAdmin(put("/api/v1/users/" + userId), """
                {"firstNames":"Ana","lastNames":"Editada","email":"%s","phone":"3150000000",
                 "homeAddress":"Calle Nueva","role":"ADMIN"}
                """.formatted(newEmail.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.lastNames").value("Editada"))
                .andExpect(jsonPath("$.guardian.homeAddress").value("Calle Nueva"))
                .andExpect(jsonPath("$.role").value("GUARDIAN"));

        loginRequest(newEmail, TestAccounts.VALID_PASSWORD).andExpect(status().isOk());
        loginRequest(email, TestAccounts.VALID_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void updateRejectsEmailOfAnotherAccount() throws Exception {
        int userId = registerGuardian(TestAccounts.uniqueEmail());

        asAdmin(put("/api/v1/users/" + userId), """
                {"firstNames":"Ana","lastNames":"Prueba","email":"%s","phone":"300"}
                """.formatted(TestAccounts.ADMIN_EMAIL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void deactivatedUserCannotLogInUntilReactivated() throws Exception {
        String email = TestAccounts.uniqueEmail();
        int userId = registerGuardian(email);

        asAdmin(patch("/api/v1/users/" + userId + "/status"), "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        loginRequest(email, TestAccounts.VALID_PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_INACTIVE"));

        asAdmin(patch("/api/v1/users/" + userId + "/status"), "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isOk());
        loginRequest(email, TestAccounts.VALID_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void adminCannotDeactivateOwnAccount() throws Exception {
        asAdmin(patch("/api/v1/users/" + adminId + "/status"), "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_SELF"));
    }

    @Test
    void statusChangeValidatesInputAndExistence() throws Exception {
        asAdmin(patch("/api/v1/users/" + adminId + "/status"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("status"));
        asAdmin(patch("/api/v1/users/999999999/status"), "{\"status\":\"ACTIVE\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveCoachesAreListedOnlyWithTheInactiveFilter() throws Exception {
        String created = asAdmin(post("/api/v1/users"), staffJson("COACH", TestAccounts.uniqueEmail(),
                TestAccounts.uniqueIdNumber(), TestAccounts.VALID_PASSWORD))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int userId = JsonPath.read(created, "$.id");
        int coachId = JsonPath.read(created, "$.coach.id");
        asAdmin(patch("/api/v1/users/" + userId + "/status"), "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());

        asAdmin(get("/api/v1/coaches").param("status", "INACTIVE"))
                .andExpect(jsonPath("$[*].id", hasItem(coachId)));
        asAdmin(get("/api/v1/coaches").param("status", "ACTIVE"))
                .andExpect(jsonPath("$[*].id", not(hasItem(coachId))));
        asAdmin(get("/api/v1/coaches"))
                .andExpect(jsonPath("$[*].id", hasItem(coachId)));
    }

    @Test
    void guardianListSearchesByEmailAndReturnsMinimalData() throws Exception {
        String email = TestAccounts.uniqueEmail();
        int userId = registerGuardian(email);

        asAdmin(get("/api/v1/guardians").param("q", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(userId))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana María Prueba Test"))
                .andExpect(jsonPath("$.content[0].email").value(email))
                .andExpect(jsonPath("$.content[0].phone").value("3001234567"))
                .andExpect(jsonPath("$.content[0].idNumber").doesNotExist())
                .andExpect(jsonPath("$.content[0].homeAddress").doesNotExist());
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder builder)
            throws Exception {
        return mockMvc.perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken));
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder builder,
                                  String body) throws Exception {
        return asAdmin(builder.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions perform(HttpMethod method, String path, String body, String token) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, path);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(builder);
    }

    private int registerGuardian(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(),
                                TestAccounts.VALID_PASSWORD)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String registerGuardianAndLogin(String email) throws Exception {
        registerGuardian(email);
        return token(email);
    }

    private String loginAsNewCoach() throws Exception {
        String email = TestAccounts.uniqueEmail();
        asAdmin(post("/api/v1/users"), staffJson("COACH", email, TestAccounts.uniqueIdNumber(),
                TestAccounts.VALID_PASSWORD)).andExpect(status().isCreated());
        return token(email);
    }

    private String token(String email) throws Exception {
        String response = loginRequest(email, TestAccounts.VALID_PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private static String staffJson(String role, String email, String idNumber, String password) {
        return """
                {"role":"%s","firstNames":"Luis","lastNames":"Personal","idType":"CC","idNumber":"%s",
                 "email":"%s","phone":"3200000000","password":"%s","specialty":"Fútbol","yearsOfExperience":5}
                """.formatted(role, idNumber, email, password);
    }
}
