package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.CoachRepository;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import com.uniquindio.teen_sport.security.JwtTokenService;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU29: U3 {@code GET /users/me}, U4 {@code PUT /users/me} y U5 {@code PUT /users/me/password}.
 */
@IntegrationTest
class MyAccountIntegrationTest {

    private static final String ME = "/api/v1/users/me";
    private static final String MY_PASSWORD = "/api/v1/users/me/password";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CoachRepository coachRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenService tokenService;

    @Test
    void allOwnAccountEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get(ME)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(put(ME).contentType(MediaType.APPLICATION_JSON).content(profileJson("", "")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(MY_PASSWORD).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guardianSeesOwnProfileWithoutPasswordData() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String token = registerAndLogin(email);

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.guardian.homeAddress").value("Calle 1 # 2-3"))
                .andExpect(jsonPath("$.coach").isEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void adminSeesOwnProfile() throws Exception {
        String token = login(TestAccounts.ADMIN_EMAIL, TestAccounts.ADMIN_PASSWORD);

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TestAccounts.ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.guardian").isEmpty())
                .andExpect(jsonPath("$.coach").isEmpty());
    }

    @Test
    void guardianUpdatesProfileWithoutChangingProtectedFields() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String token = registerAndLogin(email);
        String idNumber = userRepository.findByEmail(email).orElseThrow().getDocumentNumber();
        String body = """
                {"firstNames":"Ana Lucía","lastNames":"Gómez","phone":"3110000000","homeAddress":"Carrera 5 # 6-7",
                 "specialty":"Fútbol","yearsOfExperience":3,
                 "email":"otro@teensport.test","idNumber":"1","role":"ADMIN","status":"INACTIVE"}
                """;

        mockMvc.perform(put(ME).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstNames").value("Ana Lucía"))
                .andExpect(jsonPath("$.phone").value("3110000000"))
                .andExpect(jsonPath("$.guardian.homeAddress").value("Carrera 5 # 6-7"))
                .andExpect(jsonPath("$.coach").isEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.idNumber").value(idNumber))
                .andExpect(jsonPath("$.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.lastNames").value("Gómez"))
                .andExpect(jsonPath("$.guardian.homeAddress").value("Carrera 5 # 6-7"));
    }

    @Test
    void omittedAddressIsClearedBecausePutReplacesTheProfile() throws Exception {
        String token = registerAndLogin(TestAccounts.uniqueEmail());

        mockMvc.perform(put(ME).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(profileJson("Ana", "Gómez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardian.homeAddress").isEmpty());
    }

    @Test
    void coachUpdatesSpecialtyAndExperience() throws Exception {
        String token = coachToken();

        mockMvc.perform(put(ME).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"firstNames":"Luis","lastNames":"Pérez","phone":"3200000000",
                                 "homeAddress":"Calle 9","specialty":"Natación","yearsOfExperience":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COACH"))
                .andExpect(jsonPath("$.coach.specialty").value("Natación"))
                .andExpect(jsonPath("$.coach.yearsOfExperience").value(8))
                .andExpect(jsonPath("$.guardian").isEmpty());
    }

    @Test
    void invalidProfileDataIsAValidationError() throws Exception {
        String token = registerAndLogin(TestAccounts.uniqueEmail());

        mockMvc.perform(put(ME).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstNames\":\"\",\"lastNames\":\"Gómez\",\"phone\":\"300\",\"yearsOfExperience\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field=='firstNames')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='yearsOfExperience')]").exists());
    }

    @Test
    void changedPasswordReplacesTheOldOneForLogin() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String token = registerAndLogin(email);

        changePassword(token, TestAccounts.VALID_PASSWORD, "Nueva#Clave2026").andExpect(status().isNoContent());

        loginRequest(email, TestAccounts.VALID_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        loginRequest(email, "Nueva#Clave2026").andExpect(status().isOk());
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        String token = registerAndLogin(TestAccounts.uniqueEmail());

        changePassword(token, "Otra#Clave2026", "Nueva#Clave2026")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INCORRECT_CURRENT_PASSWORD"));
    }

    @Test
    void weakNewPasswordIsRejected() throws Exception {
        String token = registerAndLogin(TestAccounts.uniqueEmail());

        changePassword(token, TestAccounts.VALID_PASSWORD, "sinmayuscula2026#")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void shortNewPasswordIsAValidationError() throws Exception {
        String token = registerAndLogin(TestAccounts.uniqueEmail());

        changePassword(token, TestAccounts.VALID_PASSWORD, "Ab#1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("newPassword"));
    }

    @Test
    void tokenOfAnAccountThatNoLongerExistsIsUnauthenticated() throws Exception {
        String token = tokenService.issue(new AuthenticatedUser(999_999_999, RoleName.GUARDIAN, null, null));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(),
                                TestAccounts.VALID_PASSWORD)))
                .andExpect(status().isCreated());
        return login(email, TestAccounts.VALID_PASSWORD);
    }

    /**
     * Los entrenadores se crean por U8 (HU30); mientras tanto la prueba los crea directamente en la
     * base de datos y emite su token con el servicio real.
     */
    private String coachToken() {
        User user = new User(roleRepository.findByName(RoleName.COACH).orElseThrow(), TestAccounts.uniqueEmail(),
                passwordEncoder.encode(TestAccounts.VALID_PASSWORD), Instant.now());
        user.setDocumentType("CC");
        user.setDocumentNumber(TestAccounts.uniqueIdNumber());
        user.setFirstName("Luis");
        user.setLastName("Entrenador");
        userRepository.save(user);
        Coach coach = coachRepository.save(new Coach(user, "Fútbol", 2));
        return tokenService.issue(new AuthenticatedUser(user.getId(), RoleName.COACH, null, coach.getId()));
    }

    private String login(String email, String password) throws Exception {
        String response = loginRequest(email, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private ResultActions changePassword(String token, String current, String next) throws Exception {
        return mockMvc.perform(put(MY_PASSWORD).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next)));
    }

    private static String profileJson(String firstNames, String lastNames) {
        return "{\"firstNames\":\"%s\",\"lastNames\":\"%s\",\"phone\":\"300\"}".formatted(firstNames, lastNames);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
