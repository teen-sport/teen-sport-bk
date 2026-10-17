package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * U1 {@code POST /auth/login} contra la API real, con PostgreSQL.
 */
@IntegrationTest
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;

    @Test
    void registeredGuardianLogsInAndTheTokenOpensProtectedRoutes() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String registered = mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(),
                                TestAccounts.VALID_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int guardianId = JsonPath.read(registered, "$.guardian.id");

        String login = login(email.toUpperCase(), TestAccounts.VALID_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.user.firstNames").value("Ana María"))
                .andExpect(jsonPath("$.user.guardianId").value(guardianId))
                .andExpect(jsonPath("$.user.coachId").isEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(login, "$.accessToken");
        // Ruta protegida inexistente: con token válido pasa la seguridad y llega al 404 de la API.
        mockMvc.perform(get("/api/v1/protected-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void initialAdminLogsInWithRoleAdmin() throws Exception {
        login(TestAccounts.ADMIN_EMAIL, TestAccounts.ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.guardianId").isEmpty())
                .andExpect(jsonPath("$.user.coachId").isEmpty());
    }

    @Test
    void wrongPasswordAndUnknownEmailReturnTheSameError() throws Exception {
        String wrongPassword = login(TestAccounts.ADMIN_EMAIL, "Otra#Clave2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login(TestAccounts.uniqueEmail(), TestAccounts.ADMIN_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        String expectedMessage = JsonPath.read(wrongPassword, "$.message");
        assertThat((String) JsonPath.read(unknownEmail, "$.message"))
                .isEqualTo(expectedMessage);
    }

    @Test
    void inactiveUserCannotLogIn() throws Exception {
        String email = TestAccounts.uniqueEmail();
        mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(),
                                TestAccounts.VALID_PASSWORD)))
                .andExpect(status().isCreated());
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(EntityStatus.INACTIVE);
        userRepository.save(user);

        login(email, TestAccounts.VALID_PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_INACTIVE"));
    }

    @Test
    void missingCredentialsAreAValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }
}
