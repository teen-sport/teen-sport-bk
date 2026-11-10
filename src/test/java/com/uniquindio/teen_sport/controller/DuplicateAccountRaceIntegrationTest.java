package com.uniquindio.teen_sport.controller;

import com.jayway.jsonpath.JsonPath;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.support.AuthTestClient;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Duplicados simultáneos de correo o documento (RNF-11) contra PostgreSQL real.
 *
 * <p>Cada prueba reproduce la carrera de forma determinista: una transacción de la prueba inserta
 * una cuenta y queda sin confirmar, de modo que la solicitud HTTP supera la comprobación previa
 * (no ve la fila) y queda bloqueada en el índice único. Al confirmar la primera transacción,
 * PostgreSQL rechaza la escritura de la solicitud, que debe responder {@code 409} y no {@code 500}.</p>
 */
@IntegrationTest
class DuplicateAccountRaceIntegrationTest {

    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void shutDown() {
        executor.shutdownNow();
    }

    @Test
    void registrationRacingAnotherAccountWithTheSameEmailIsAnEmailConflict() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String idNumber = TestAccounts.uniqueIdNumber();

        MvcResult result = raceAgainstUncommittedAccount(email, TestAccounts.uniqueIdNumber(),
                () -> register(email, idNumber));

        assertConflict(result, "EMAIL_ALREADY_REGISTERED");
        assertThat(userRepository.existsByDocumentNumber(idNumber)).isFalse();
    }

    @Test
    void registrationRacingAnotherAccountWithTheSameDocumentIsADocumentConflict() throws Exception {
        String email = TestAccounts.uniqueEmail();
        String idNumber = TestAccounts.uniqueIdNumber();

        MvcResult result = raceAgainstUncommittedAccount(TestAccounts.uniqueEmail(), idNumber,
                () -> register(email, idNumber));

        assertConflict(result, "ID_NUMBER_ALREADY_REGISTERED");
        assertThat(userRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void emailChangeRacingAnotherAccountWithTheSameEmailIsAnEmailConflict() throws Exception {
        String adminToken = new AuthTestClient(mockMvc).adminToken();
        String originalEmail = TestAccounts.uniqueEmail();
        int userId = registeredUserId(originalEmail);
        String takenEmail = TestAccounts.uniqueEmail();

        MvcResult result = raceAgainstUncommittedAccount(takenEmail, TestAccounts.uniqueIdNumber(),
                () -> mockMvc.perform(put("/api/v1/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, AuthTestClient.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstNames":"Ana","lastNames":"Prueba","email":"%s","phone":"300"}
                                """.formatted(takenEmail))).andReturn());

        assertConflict(result, "EMAIL_ALREADY_REGISTERED");
        assertThat(userRepository.findById(userId)).get().extracting(User::getEmail).isEqualTo(originalEmail);
    }

    /**
     * Inserta una cuenta sin confirmar, lanza la solicitud, espera a que quede bloqueada por esa fila
     * y entonces confirma la inserción.
     */
    private MvcResult raceAgainstUncommittedAccount(String email, String idNumber, Callable<MvcResult> request)
            throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> {
                    userRepository.saveAndFlush(account(email, idNumber));
                    inserted.countDown();
                    await(release);
                }));
        await(inserted);

        Future<MvcResult> response = executor.submit(request);
        awaitBlockedOnLock();
        release.countDown();
        holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(userRepository.existsByEmail(email)).isTrue();
        return response.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void awaitBlockedOnLock() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbcTemplate.queryForObject("SELECT count(*) FROM pg_locks WHERE NOT granted",
                    Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        fail("La solicitud no llegó a esperar por la fila sin confirmar");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tiempo de espera agotado");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private User account(String email, String idNumber) {
        User user = new User(roleRepository.findByName(RoleName.GUARDIAN).orElseThrow(), email, "$2a$hash",
                Instant.now());
        user.setDocumentType("CC");
        user.setDocumentNumber(idNumber);
        user.setFirstName("Cuenta");
        user.setLastName("Concurrente");
        return user;
    }

    private MvcResult register(String email, String idNumber) throws Exception {
        return mockMvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                .content(TestAccounts.registrationJson(email, idNumber, TestAccounts.VALID_PASSWORD))).andReturn();
    }

    private int registeredUserId(String email) throws Exception {
        MvcResult result = register(email, TestAccounts.uniqueIdNumber());
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private static void assertConflict(MvcResult result, String code) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(body, "$.code")).isEqualTo(code);
        assertThat(body).doesNotContainIgnoringCase("constraint").doesNotContainIgnoringCase("duplicate key");
    }
}
