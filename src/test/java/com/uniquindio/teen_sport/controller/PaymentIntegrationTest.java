package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.support.AcademyTestClient;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.ConcurrentRequests;
import com.uniquindio.teen_sport.support.IntegrationTest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.uniquindio.teen_sport.support.AuthTestClient.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU36: pagos presenciales (E16–E19 y el pago de E12) contra la API real con PostgreSQL: reglas del
 * pago, permisos por rol y por propiedad, y registros simultáneos del mismo mes.
 */
@IntegrationTest
class PaymentIntegrationTest {

    private static final String ENROLLMENTS = "/api/v1/enrollments/";
    private static final String PAYMENTS = "/api/v1/payments";
    private static final ZoneId ACADEMY_ZONE = ZoneId.of("America/Bogota");
    private static final String FEE = "100000";

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;
    private AcademyTestClient academy;
    private String adminToken;
    private int groupId;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthTestClient(mockMvc);
        adminToken = auth.adminToken();
        academy = new AcademyTestClient(mockMvc, adminToken);
        groupId = academy.newGroup(academy.newDiscipline(), auth.newCoach().coachId());
        today = LocalDate.now(ACADEMY_ZONE);
    }

    @Test
    void reportDoesNotChangeTheStatusAndRegisteringLeavesTheEnrollmentUpToDate() throws Exception {
        String token = auth.newGuardianToken();
        int enrollmentId = enroll(token);

        send(post(ENROLLMENTS + enrollmentId + "/payment-report"), token, "{\"notes\":\"Pagué en caja\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReported").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("OVERDUE"));
        send(post(ENROLLMENTS + enrollmentId + "/payments"), adminToken,
                paymentJson(FEE, today, "\"receiptReference\":\"R-001\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value(enrollmentId))
                .andExpect(jsonPath("$.amountPaid").value(100000))
                .andExpect(jsonPath("$.paymentDate").value(today.toString()))
                .andExpect(jsonPath("$.period").value(YearMonth.from(today).toString()))
                .andExpect(jsonPath("$.receiptReference").value("R-001"))
                .andExpect(jsonPath("$.registeredBy.fullName").isNotEmpty())
                .andExpect(jsonPath("$.athlete.firstNames").isNotEmpty());

        send(get(ENROLLMENTS + enrollmentId), token, null)
                .andExpect(jsonPath("$.paymentStatus").value("UP_TO_DATE"))
                .andExpect(jsonPath("$.paymentReported").value(false))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        send(post(ENROLLMENTS + enrollmentId + "/payment-report"), token, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_REGISTERED"));
        send(post(ENROLLMENTS + enrollmentId + "/payments"), adminToken, paymentJson("150000", today, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_REGISTERED"));
        send(post(ENROLLMENTS + enrollmentId + "/payments"), adminToken, paymentJson(FEE, today.minusMonths(1), null))
                .andExpect(status().isCreated());
    }

    @Test
    void paymentRulesAndInputLimits() throws Exception {
        int enrollmentId = enroll(auth.newGuardianToken());
        String url = ENROLLMENTS + enrollmentId + "/payments";

        send(post(url), adminToken, paymentJson("99999.99", today, null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_AMOUNT"));
        send(post(url), adminToken, paymentJson(FEE, today.plusDays(1), null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("FUTURE_DATE"));
        send(post(url), adminToken, paymentJson("0", today, null)).andExpect(status().isBadRequest());
        send(post(url), adminToken, paymentJson("100000.001", today, null)).andExpect(status().isBadRequest());
        send(post(url), adminToken, paymentJson(FEE, today, "\"receiptReference\":\"" + "R".repeat(51) + "\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("receiptReference"));
        send(post(url), adminToken, "{\"paymentDate\":\"" + today + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("amountPaid"));
        send(post(ENROLLMENTS + "999999999/payments"), adminToken, paymentJson(FEE, today, null))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelledEnrollmentsAcceptNoPaymentsButCanBeMarkedOverdue() throws Exception {
        String token = auth.newGuardianToken();
        int enrollmentId = enroll(token);
        send(post(ENROLLMENTS + enrollmentId + "/cancel"), token, null).andExpect(status().isOk());

        send(post(ENROLLMENTS + enrollmentId + "/payments"), adminToken, paymentJson(FEE, today, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        send(post(ENROLLMENTS + enrollmentId + "/payment-report"), token, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        send(patch(ENROLLMENTS + enrollmentId + "/payment-status"), adminToken, "{\"paymentStatus\":\"OVERDUE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("OVERDUE"));
    }

    @Test
    void overdueIsManualButUpToDateNeedsAPayment() throws Exception {
        int enrollmentId = enroll(auth.newGuardianToken());
        String url = ENROLLMENTS + enrollmentId + "/payment-status";
        send(post(ENROLLMENTS + enrollmentId + "/payments"), adminToken, paymentJson(FEE, today, null))
                .andExpect(status().isCreated());

        send(patch(url), adminToken, "{\"paymentStatus\":\"UP_TO_DATE\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("USE_PAYMENT_REGISTRATION"));
        send(patch(url), adminToken, "{\"paymentStatus\":\"PAID\"}").andExpect(status().isBadRequest());
        send(patch(url), adminToken, "{}").andExpect(status().isBadRequest());
        send(patch(url), adminToken, "{\"paymentStatus\":\"OVERDUE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("OVERDUE"));
    }

    @Test
    void confirmCanRegisterThePaymentInTheSameOperation() throws Exception {
        String token = auth.newGuardianToken();
        int paid = enroll(token);
        int rejectedPayment = enroll(token);

        send(post(ENROLLMENTS + rejectedPayment + "/confirm"), adminToken,
                "{\"payment\":" + paymentJson("1000", today, null) + "}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_AMOUNT"));
        send(get(ENROLLMENTS + rejectedPayment), token, null).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        send(post(ENROLLMENTS + rejectedPayment + "/confirm"), adminToken, "{\"payment\":{\"paymentDate\":\"" + today + "\"}}")
                .andExpect(status().isBadRequest());

        send(post(ENROLLMENTS + paid + "/confirm"), adminToken, "{\"payment\":" + paymentJson(FEE, today, null) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.paymentStatus").value("UP_TO_DATE"));
        send(get(PAYMENTS + "?enrollmentId=" + paid), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void onlyGuardiansReportAndOnlyAdminsRegisterOrMarkOverdue() throws Exception {
        String owner = auth.newGuardianToken();
        String other = auth.newGuardianToken();
        String coach = auth.newCoachToken();
        int enrollmentId = enroll(owner);

        send(post(ENROLLMENTS + enrollmentId + "/payment-report"), other, null).andExpect(status().isForbidden());
        send(post(ENROLLMENTS + enrollmentId + "/payment-report"), adminToken, null).andExpect(status().isForbidden());
        for (String token : new String[]{owner, coach}) {
            send(post(ENROLLMENTS + enrollmentId + "/payments"), token, "{}").andExpect(status().isForbidden());
            send(patch(ENROLLMENTS + enrollmentId + "/payment-status"), token, "{}").andExpect(status().isForbidden());
        }
        send(post(ENROLLMENTS + enrollmentId + "/payment-report"), coach, null).andExpect(status().isForbidden());
        send(get(PAYMENTS), coach, null).andExpect(status().isForbidden());
        mockMvc.perform(get(PAYMENTS)).andExpect(status().isUnauthorized());
        send(get(ENROLLMENTS + enrollmentId), owner, null).andExpect(jsonPath("$.paymentReported").value(false));
    }

    @Test
    void guardiansOnlySeeTheirOwnPayments() throws Exception {
        String owner = auth.newGuardianToken();
        String other = auth.newGuardianToken();
        int athleteId = academy.newAthlete(owner);
        int enrollmentId = enroll(owner, athleteId);
        send(post(ENROLLMENTS + enrollmentId + "/payments"), adminToken, paymentJson(FEE, today, null))
                .andExpect(status().isCreated());

        send(get(PAYMENTS), owner, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].enrollmentId").value(enrollmentId));
        send(get(PAYMENTS + "?athleteId=" + athleteId), other, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        send(get(PAYMENTS + "?enrollmentId=" + enrollmentId + "&from=" + today + "&to=" + today), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(1));
        send(get(PAYMENTS + "?enrollmentId=" + enrollmentId + "&from=" + today.plusDays(1)), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(0));
        send(get(PAYMENTS + "?from=" + today + "&to=" + today.minusDays(1)), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("from"));
        send(get(PAYMENTS + "?size=101"), adminToken, null).andExpect(status().isBadRequest());
    }

    @Test
    void simultaneousPaymentsForTheSameMonthSaveOnlyOne() throws Exception {
        int enrollmentId = enroll(auth.newGuardianToken());
        String url = ENROLLMENTS + enrollmentId + "/payments";

        List<Integer> statuses = ConcurrentRequests.run(
                () -> statusOf(send(post(url), adminToken, paymentJson(FEE, today, null))),
                () -> statusOf(send(post(url), adminToken, paymentJson(FEE, today.withDayOfMonth(1), null))));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        send(get(PAYMENTS + "?enrollmentId=" + enrollmentId), adminToken, null)
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private int enroll(String token) throws Exception {
        return enroll(token, academy.newAthlete(token));
    }

    private int enroll(String token, int athleteId) throws Exception {
        ResultActions result = academy.requestEnrollment(token, athleteId, groupId, YearMonth.from(today).toString());
        return JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.id");
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private static String paymentJson(String amount, LocalDate date, String extra) {
        return "{\"amountPaid\":" + amount + ",\"paymentDate\":\"" + date + "\"" + (extra == null ? "" : "," + extra)
                + "}";
    }

    private static int statusOf(ResultActions result) {
        return result.andReturn().getResponse().getStatus();
    }
}
