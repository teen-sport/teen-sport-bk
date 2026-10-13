package com.uniquindio.teen_sport.support;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prepara, a través de la API, los datos de academia que necesitan las pruebas: disciplinas, grupos,
 * franjas y deportistas, y envía solicitudes de matrícula. Usa nombres únicos para no chocar con otras
 * pruebas.
 */
public class AcademyTestClient {

    private final MockMvc mockMvc;
    private final String adminToken;

    /**
     * @param mockMvc    cliente HTTP de la prueba
     * @param adminToken token de administrador
     */
    public AcademyTestClient(MockMvc mockMvc, String adminToken) {
        this.mockMvc = mockMvc;
        this.adminToken = adminToken;
    }

    /**
     * @return id de una disciplina activa nueva
     * @throws Exception si la solicitud falla
     */
    public int newDiscipline() throws Exception {
        String body = "{\"name\":\"Disciplina %s\",\"monthlyFee\":100000}".formatted(UUID.randomUUID());
        return idOf(mockMvc.perform(post("/api/v1/disciplines").header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    /**
     * @param disciplineId disciplina
     * @param coachId      entrenador
     * @return id de un grupo activo nuevo, con capacidad 15
     * @throws Exception si la solicitud falla
     */
    public int newGroup(int disciplineId, int coachId) throws Exception {
        return newGroup(disciplineId, coachId, 15);
    }

    /**
     * @param disciplineId disciplina
     * @param coachId      entrenador
     * @param capacity     capacidad máxima
     * @return id de un grupo activo nuevo
     * @throws Exception si la solicitud falla
     */
    public int newGroup(int disciplineId, int coachId, int capacity) throws Exception {
        String body = "{\"disciplineId\":%d,\"coachId\":%d,\"name\":\"Grupo %s\",\"maxCapacity\":%d}"
                .formatted(disciplineId, coachId, UUID.randomUUID().toString().substring(0, 8), capacity);
        return idOf(mockMvc.perform(post("/api/v1/groups").header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    /**
     * @param groupId grupo
     * @param day     día ({@code MONDAY} a {@code SUNDAY})
     * @param start   hora de inicio {@code HH:mm}
     * @param end     hora de fin {@code HH:mm}
     * @return la respuesta de H12, sin verificar
     * @throws Exception si la solicitud falla
     */
    public ResultActions addSchedule(int groupId, String day, String start, String end) throws Exception {
        return mockMvc.perform(post("/api/v1/groups/" + groupId + "/schedules")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson(day, start, end)));
    }

    /**
     * Registra un deportista de 14 años para el acudiente del token.
     *
     * @param guardianToken token del acudiente
     * @return id del deportista
     * @throws Exception si la solicitud falla
     */
    public int newAthlete(String guardianToken) throws Exception {
        String body = """
                {"firstNames":"Sofía","lastNames":"Prueba","idType":"TI","idNumber":"%s","birthDate":"%s"}
                """.formatted(TestAccounts.uniqueIdNumber(), LocalDate.now(ZoneId.of("America/Bogota")).minusYears(14));
        return idOf(mockMvc.perform(post("/api/v1/athletes").header(HttpHeaders.AUTHORIZATION, bearer(guardianToken))
                .contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    /**
     * @param token     token de quien solicita
     * @param athleteId deportista
     * @param groupId   grupo
     * @param period    mes de inicio {@code YYYY-MM}
     * @return la respuesta de E6, sin verificar
     * @throws Exception si la solicitud falla
     */
    public ResultActions requestEnrollment(String token, int athleteId, int groupId, String period) throws Exception {
        return mockMvc.perform(post("/api/v1/enrollments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"athleteId\":%d,\"groupId\":%d,\"period\":\"%s\"}".formatted(athleteId, groupId, period)));
    }

    /**
     * @param day   día
     * @param start hora de inicio
     * @param end   hora de fin
     * @return cuerpo JSON de una franja
     */
    public static String scheduleJson(String day, String start, String end) {
        return "{\"dayOfWeek\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}".formatted(day, start, end);
    }

    private static int idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.id");
    }
}
