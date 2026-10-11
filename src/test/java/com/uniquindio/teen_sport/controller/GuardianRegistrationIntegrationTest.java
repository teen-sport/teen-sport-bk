package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.GuardianRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * U2 {@code POST /users/register} contra la API real, con PostgreSQL.
 */
@IntegrationTest
class GuardianRegistrationIntegrationTest {

    private static final String REGISTER = "/api/v1/users/register";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GuardianRepository guardianRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersGuardianWithoutTokenAndNeverReturnsThePassword() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String idNumber = TestAccounts.uniqueIdNumber();

        register(TestAccounts.registrationJson(email.toUpperCase(), idNumber, TestAccounts.VALID_PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/users/")))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.idNumber").value(idNumber))
                .andExpect(jsonPath("$.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.acceptedDataPolicy").value(true))
                .andExpect(jsonPath("$.guardian.id").isNumber())
                .andExpect(jsonPath("$.guardian.homeAddress").value("Calle 1 # 2-3"))
                .andExpect(jsonPath("$.coach").doesNotExist())
                .andExpect(content().string(not(containsString(TestAccounts.VALID_PASSWORD))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));

        User stored = userRepository.findByEmail(email).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo(TestAccounts.VALID_PASSWORD);
        assertThat(passwordEncoder.matches(TestAccounts.VALID_PASSWORD, stored.getPasswordHash())).isTrue();
        assertThat(stored.getAcceptedDataPolicyAt()).isNotNull();
        assertThat(guardianRepository.findByUserId(stored.getId())).isPresent();
    }

    @Test
    void locationHeaderPointsToTheCreatedUser() throws Exception {
        String email = TestAccounts.uniqueEmail();
        register(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(), TestAccounts.VALID_PASSWORD))
                .andExpect(header().string("Location",
                        endsWith("/api/v1/users/" + userRepository.findByEmail(email).map(User::getId).orElse(-1))));
    }

    @Test
    void duplicateEmailIgnoringCaseIsAConflict() throws Exception {
        String email = TestAccounts.uniqueEmail();
        register(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(), TestAccounts.VALID_PASSWORD))
                .andExpect(status().isCreated());

        register(TestAccounts.registrationJson(email.toUpperCase(), TestAccounts.uniqueIdNumber(),
                TestAccounts.VALID_PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void duplicateIdNumberIsAConflict() throws Exception {
        String idNumber = TestAccounts.uniqueIdNumber();
        register(TestAccounts.registrationJson(TestAccounts.uniqueEmail(), idNumber, TestAccounts.VALID_PASSWORD))
                .andExpect(status().isCreated());

        register(TestAccounts.registrationJson(TestAccounts.uniqueEmail(), idNumber, TestAccounts.VALID_PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ID_NUMBER_ALREADY_REGISTERED"));
    }

    @Test
    void dataPolicyMustBeAccepted() throws Exception {
        String body = TestAccounts.registrationJson(TestAccounts.uniqueEmail(), TestAccounts.uniqueIdNumber(),
                TestAccounts.VALID_PASSWORD).replace("\"acceptedDataPolicy\":true", "\"acceptedDataPolicy\":false");

        register(body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DATA_POLICY_NOT_ACCEPTED"));
    }

    @Test
    void passwordWithoutRequiredCharactersIsWeak() throws Exception {
        register(TestAccounts.registrationJson(TestAccounts.uniqueEmail(), TestAccounts.uniqueIdNumber(), "segura2026"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void passwordShorterThanEightIsAValidationError() throws Exception {
        register(TestAccounts.registrationJson(TestAccounts.uniqueEmail(), TestAccounts.uniqueIdNumber(), "Ab#1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("password"));
    }

    @Test
    void invalidFieldsAreReportedWithoutCreatingTheAccount() throws Exception {
        register("""
                {"firstNames":"","lastNames":"Prueba","idType":"CC","idNumber":"1",
                 "email":"no-es-correo","phone":"300","password":"Segura#2026"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='firstNames')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='acceptedDataPolicy')]").exists());
    }

    @Test
    void roleSentByTheClientIsIgnored() throws Exception {
        String body = TestAccounts.registrationJson(TestAccounts.uniqueEmail(), TestAccounts.uniqueIdNumber(),
                TestAccounts.VALID_PASSWORD).replace("{", "{\"role\":\"ADMIN\",");

        register(body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("GUARDIAN"));
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
