package com.uniquindio.teen_sport.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Obtiene tokens reales de cada rol a través de la API, para las pruebas de integración.
 *
 * <p>Crea las cuentas con datos únicos de {@link TestAccounts}, por lo que puede usarse en cualquier
 * orden sin chocar con otras pruebas que comparten la base.</p>
 */
public class AuthTestClient {

    private final MockMvc mockMvc;

    /**
     * @param mockMvc cliente HTTP de la prueba
     */
    public AuthTestClient(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * @return token del administrador inicial
     * @throws Exception si la solicitud falla
     */
    public String adminToken() throws Exception {
        return login(TestAccounts.ADMIN_EMAIL, TestAccounts.ADMIN_PASSWORD);
    }

    /**
     * Registra un acudiente nuevo e inicia sesión con él.
     *
     * @return token del acudiente
     * @throws Exception si la solicitud falla
     */
    public String newGuardianToken() throws Exception {
        return newGuardian().token();
    }

    /**
     * Registra un acudiente nuevo e inicia sesión con él.
     *
     * @return token e ids del acudiente
     * @throws Exception si la solicitud falla
     */
    public TestGuardian newGuardian() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String profile = mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAccounts.registrationJson(email, TestAccounts.uniqueIdNumber(),
                                TestAccounts.VALID_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new TestGuardian(login(email, TestAccounts.VALID_PASSWORD), JsonPath.read(profile, "$.id"),
                JsonPath.read(profile, "$.guardian.id"));
    }

    /**
     * Crea un entrenador nuevo (como administrador) e inicia sesión con él.
     *
     * @return token del entrenador
     * @throws Exception si la solicitud falla
     */
    public String newCoachToken() throws Exception {
        return newCoach().token();
    }

    /**
     * Crea un entrenador nuevo (como administrador) e inicia sesión con él.
     *
     * @return token e ids del entrenador
     * @throws Exception si la solicitud falla
     */
    public TestCoach newCoach() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String profile = mockMvc.perform(post("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"COACH","firstNames":"Luis","lastNames":"Entrenador","idType":"CC",
                                 "idNumber":"%s","email":"%s","phone":"3200000000","password":"%s"}
                                """.formatted(TestAccounts.uniqueIdNumber(), email, TestAccounts.VALID_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new TestCoach(login(email, TestAccounts.VALID_PASSWORD), JsonPath.read(profile, "$.id"),
                JsonPath.read(profile, "$.coach.id"));
    }

    /**
     * @param token JWT
     * @return valor de la cabecera {@code Authorization}
     */
    public static String bearer(String token) {
        return "Bearer " + token;
    }

    /**
     * Acudiente creado para una prueba.
     *
     * @param token      JWT del acudiente
     * @param userId     id de su cuenta
     * @param guardianId id de su perfil de acudiente
     */
    public record TestGuardian(String token, int userId, int guardianId) {
    }

    /**
     * Entrenador creado para una prueba.
     *
     * @param token   JWT del entrenador
     * @param userId  id de su cuenta
     * @param coachId id de su perfil de entrenador
     */
    public record TestCoach(String token, int userId, int coachId) {
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
