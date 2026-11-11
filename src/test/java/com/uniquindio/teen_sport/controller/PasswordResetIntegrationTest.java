package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.integration.EmailMessage;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient.TestGuardian;
import com.uniquindio.teen_sport.support.ConcurrentRequests;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.RecordingEmailSender;
import com.uniquindio.teen_sport.support.TestAccounts;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * U13 y U14 de punta a punta: endpoints públicos, tokens en PostgreSQL y correo en memoria.
 */
@IntegrationTest
class PasswordResetIntegrationTest {

    private static final String REQUEST = "/api/v1/auth/password-reset/request";
    private static final String CONFIRM = "/api/v1/auth/password-reset/confirm";
    private static final String NEW_PASSWORD = "Recuperada#2026";
    private static final Pattern TOKEN_IN_LINK = Pattern.compile("reset-password\\?token=([A-Za-z0-9_-]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingEmailSender emailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestGuardian guardian;
    private String email;

    @BeforeEach
    void setUp() throws Exception {
        guardian = new AuthTestClient(mockMvc).newGuardian();
        email = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, guardian.userId());
    }

    @Test
    void theEmailedTokenResetsThePasswordOnlyOnce() throws Exception {
        requestReset(email.toUpperCase()).andExpect(status().isAccepted()).andExpect(content().string(""));
        String token = lastToken();

        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());

        login(TestAccounts.VALID_PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
        confirm(token, "Otra#Clave2026").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        assertThat(notifications()).containsExactly("ACCOUNT_REGISTERED SENT 1", "PASSWORD_RESET SENT 1");
        assertThat(storedHashes()).singleElement().asString().hasSize(64).isNotEqualTo(token);
    }

    @Test
    void unknownAndInactiveAccountsGetTheSameAnswerAndNoEmail() throws Exception {
        String unknown = TestAccounts.uniqueEmail();
        requestReset(unknown).andExpect(status().isAccepted()).andExpect(content().string(""));
        assertThat(emailSender.sentTo(unknown)).isEmpty();

        String adminToken = new AuthTestClient(mockMvc).adminToken();
        mockMvc.perform(patch("/api/v1/users/" + guardian.userId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        requestReset(email).andExpect(status().isAccepted()).andExpect(content().string(""));

        assertThat(emailSender.sentTo(email)).extracting(EmailMessage::subject)
                .containsExactly("Bienvenido a TeenSport");
        assertThat(storedHashes()).isEmpty();
    }

    @Test
    void aNewRequestInvalidatesThePreviousLink() throws Exception {
        requestReset(email).andExpect(status().isAccepted());
        String first = lastToken();
        requestReset(email).andExpect(status().isAccepted());
        String second = lastToken();

        confirm(first, NEW_PASSWORD).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        confirm(second, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void anExpiredLinkIsRejected() throws Exception {
        requestReset(email).andExpect(status().isAccepted());
        String token = lastToken();
        jdbcTemplate.update("update password_reset_tokens set expires_at = expires_at - interval '31 minutes' "
                + "where user_id = ?", guardian.userId());

        confirm(token, NEW_PASSWORD).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        login(TestAccounts.VALID_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void aWeakPasswordDoesNotSpendTheLink() throws Exception {
        requestReset(email).andExpect(status().isAccepted());
        String token = lastToken();

        confirm(token, "sinsimbolos123").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void invalidInputIsRejectedWithoutAToken() throws Exception {
        requestReset("no-es-un-correo").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        confirm(" ", NEW_PASSWORD).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        confirm("x".repeat(101), NEW_PASSWORD).andExpect(status().isBadRequest());
        confirm("token-inventado", "corta").andExpect(status().isBadRequest());
        confirm("token-inventado", NEW_PASSWORD).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"))
                .andExpect(jsonPath("$.message").value(not(containsString("token-inventado"))));
    }

    @Test
    void twoSimultaneousResetsWithTheSameLinkSucceedOnce() throws Exception {
        requestReset(email).andExpect(status().isAccepted());
        String token = lastToken();

        List<Integer> statuses = ConcurrentRequests.run(
                () -> confirm(token, NEW_PASSWORD).andReturn().getResponse().getStatus(),
                () -> confirm(token, "Distinta#2026").andReturn().getResponse().getStatus());

        assertThat(statuses).containsExactlyInAnyOrder(204, 422);
    }

    private ResultActions requestReset(String address) throws Exception {
        return mockMvc.perform(post(REQUEST).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(address)));
    }

    private ResultActions confirm(String token, String password) throws Exception {
        return mockMvc.perform(post(CONFIRM).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, password)));
    }

    private ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String lastToken() {
        List<EmailMessage> messages = emailSender.sentTo(email);
        EmailMessage last = messages.get(messages.size() - 1);
        assertThat(last.subject()).isEqualTo("Recuperación de contraseña");
        Matcher matcher = TOKEN_IN_LINK.matcher(last.htmlContent());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private List<String> notifications() {
        return jdbcTemplate.queryForList("""
                select type || ' ' || delivery_status || ' ' || attempts from notifications
                where user_id = ? order by id
                """, String.class, guardian.userId());
    }

    private List<String> storedHashes() {
        return jdbcTemplate.queryForList("select token_hash from password_reset_tokens where user_id = ?",
                String.class, guardian.userId());
    }
}
