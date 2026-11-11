package com.uniquindio.teen_sport.integration;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Adaptador de Brevo contra un servidor simulado: nunca llama a Brevo real.
 */
class BrevoEmailSenderTest {

    private static final String BASE_URL = "https://brevo.test/v3";
    private static final EmailMessage MESSAGE =
            new EmailMessage("ana@teensport.test", "Ana Gómez", "Matrícula confirmada", "<p>Hola</p>");

    private MockRestServiceServer server;
    private BrevoEmailSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoEmailSender(builder.build(), properties("xkeysib-test", "no-reply@teensport.test"));
    }

    @Test
    void sendsTheTransactionalEmailWithTheApiKeyAndOnlyTheNeededData() {
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-test"))
                .andExpect(jsonPath("$.sender.email").value("no-reply@teensport.test"))
                .andExpect(jsonPath("$.sender.name").value("TeenSport"))
                .andExpect(jsonPath("$.to[0].email").value("ana@teensport.test"))
                .andExpect(jsonPath("$.to[0].name").value("Ana Gómez"))
                .andExpect(jsonPath("$.subject").value("Matrícula confirmada"))
                .andExpect(jsonPath("$.htmlContent").value("<p>Hola</p>"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"<1@smtp-relay.brevo.com>\"}"));

        sender.send(MESSAGE);

        server.verify();
    }

    @Test
    void clientErrorsArePermanent() {
        server.expect(requestTo(BASE_URL + "/smtp/email")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        EmailDeliveryException failure = failure();

        assertThat(failure.isRetryable()).isFalse();
        assertThat(failure.getMessage()).contains("401").doesNotContain("xkeysib");
    }

    @Test
    void rateLimitsAreTransient() {
        server.expect(requestTo(BASE_URL + "/smtp/email")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(failure().isRetryable()).isTrue();
    }

    @Test
    void providerErrorsAreTransient() {
        server.expect(requestTo(BASE_URL + "/smtp/email")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(failure().isRetryable()).isTrue();
    }

    @Test
    void networkErrorsAreTransientAndKeepTheCause() {
        server.expect(requestTo(BASE_URL + "/smtp/email")).andRespond(withException(new IOException("timeout")));

        EmailDeliveryException failure = failure();

        assertThat(failure.isRetryable()).isTrue();
        assertThat(failure.getCause()).isNotNull();
    }

    @Test
    void withoutApiKeyOrSenderNothingIsSentAndTheFailureIsPermanent() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer untouched = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailSender withoutKey = new BrevoEmailSender(builder.build(), properties("", "no-reply@teensport.test"));
        BrevoEmailSender withoutSender = new BrevoEmailSender(builder.build(), properties("xkeysib-test", " "));

        assertThat(catchThrowableOfType(EmailDeliveryException.class, () -> withoutKey.send(MESSAGE)).isRetryable())
                .isFalse();
        assertThat(catchThrowableOfType(EmailDeliveryException.class, () -> withoutSender.send(MESSAGE)))
                .isNotNull();
        untouched.verify();
    }

    @Test
    void secretsAndAddressesStayOutOfTheTextRepresentations() {
        assertThat(properties("xkeysib-secret", "no-reply@teensport.test").toString()).doesNotContain("xkeysib");
        assertThat(MESSAGE.toString()).doesNotContain("ana@teensport.test").doesNotContain("Hola");
    }

    @Test
    void theConfiguredClientPointsToTheBaseUrl() {
        RestClient client = new BrevoConfiguration().brevoRestClient(properties("xkeysib-test", "a@b.test"));

        assertThat(client).isNotNull();
        assertThat(new BrevoProperties(null, null, "TeenSport", BASE_URL, Duration.ofSeconds(1),
                Duration.ofSeconds(1)).isConfigured()).isFalse();
    }

    private EmailDeliveryException failure() {
        EmailDeliveryException failure = catchThrowableOfType(EmailDeliveryException.class, () -> sender.send(MESSAGE));
        server.verify();
        return failure;
    }

    private static BrevoProperties properties(String apiKey, String senderEmail) {
        return new BrevoProperties(apiKey, senderEmail, "TeenSport", BASE_URL, Duration.ofSeconds(5),
                Duration.ofSeconds(10));
    }
}
