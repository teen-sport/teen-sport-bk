package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.PasswordResetConfirmRequest;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.PasswordResetToken;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.PasswordResetTokenRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T15:00:00Z");
    private static final Duration VALIDITY = Duration.ofMinutes(30);
    private static final String RESET_URL = "https://app.teensport.test/reset-password";
    private static final String TOKEN = "token-recibido-en-el-correo";
    private static final String NEW_PASSWORD = "Nueva#2026";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final PasswordResetService service = new PasswordResetService(userRepository, tokenRepository,
            passwordEncoder, new PasswordPolicy(), notificationService,
            new PasswordResetProperties(RESET_URL, VALIDITY), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void anActiveAccountGetsASingleUseTokenStoredOnlyAsItsHash() {
        User user = account(EntityStatus.ACTIVE);
        when(userRepository.findByEmail("ana@teensport.test")).thenReturn(Optional.of(user));

        service.requestReset("  Ana@TeenSport.test ");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(tokenRepository, notificationService);
        order.verify(tokenRepository).expireOpenTokens(10, NOW);
        order.verify(tokenRepository).save(saved.capture());
        order.verify(notificationService).passwordResetRequested(eq(user), link.capture(), eq(VALIDITY));

        assertThat(link.getValue()).startsWith(RESET_URL + "?token=");
        String token = link.getValue().substring((RESET_URL + "?token=").length());
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        PasswordResetToken stored = saved.getValue();
        assertThat(stored.getTokenHash()).isEqualTo(PasswordResetService.hash(token)).doesNotContain(token);
        assertThat(stored.getUser()).isSameAs(user);
        assertThat(stored.getCreatedAt()).isEqualTo(NOW);
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(VALIDITY));
        assertThat(stored.getUsedAt()).isNull();
    }

    @Test
    void eachRequestCreatesADifferentToken() {
        User user = account(EntityStatus.ACTIVE);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        service.requestReset(user.getEmail());
        service.requestReset(user.getEmail());

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getTokenHash()).isNotEqualTo(saved.getAllValues().get(1).getTokenHash());
    }

    @Test
    void unknownEmailsAndInactiveAccountsGetNothingAndNoError() {
        when(userRepository.findByEmail("nadie@teensport.test")).thenReturn(Optional.empty());
        User inactive = account(EntityStatus.INACTIVE);
        when(userRepository.findByEmail("ana@teensport.test")).thenReturn(Optional.of(inactive));

        service.requestReset("nadie@teensport.test");
        service.requestReset("ana@teensport.test");

        verifyNoInteractions(tokenRepository, notificationService);
    }

    @Test
    void aValidTokenChangesThePasswordConsumesItAndInvalidatesTheOthers() {
        User user = account(EntityStatus.ACTIVE);
        PasswordResetToken token = storedToken(user, NOW.plusSeconds(1), null);

        service.confirmReset(new PasswordResetConfirmRequest(TOKEN, NEW_PASSWORD));

        assertThat(passwordEncoder.matches(NEW_PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        verify(tokenRepository).expireOpenTokens(10, NOW);
    }

    @Test
    void unknownExpiredUsedOrInactiveTokensGetTheSameError() {
        when(tokenRepository.findLockedByTokenHash(anyString())).thenReturn(Optional.empty());
        assertInvalidToken();

        storedToken(account(EntityStatus.ACTIVE), NOW, null);
        assertInvalidToken();

        storedToken(account(EntityStatus.ACTIVE), NOW.plus(VALIDITY), NOW.minusSeconds(60));
        assertInvalidToken();

        storedToken(account(EntityStatus.INACTIVE), NOW.plus(VALIDITY), null);
        assertInvalidToken();

        verify(tokenRepository, never()).expireOpenTokens(anyInt(), any());
    }

    @Test
    void anInvalidTokenIsReportedBeforeAWeakPassword() {
        when(tokenRepository.findLockedByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest(TOKEN, "debilsimbolo")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESET_TOKEN));
    }

    @Test
    void aWeakPasswordKeepsTheTokenAndThePassword() {
        User user = account(EntityStatus.ACTIVE);
        String originalHash = user.getPasswordHash();
        PasswordResetToken token = storedToken(user, NOW.plus(VALIDITY), null);

        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest(TOKEN, "sinsimbolos123")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.WEAK_PASSWORD));

        assertThat(token.getUsedAt()).isNull();
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        verify(tokenRepository, never()).expireOpenTokens(anyInt(), any());
    }

    @Test
    void theTokenIsLookedUpByItsSha256Hash() {
        assertThat(PasswordResetService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void theConfigurationRequiresAPageAndAPositiveValidity() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PasswordResetProperties(" ", VALIDITY));
        assertThatIllegalArgumentException().isThrownBy(() -> new PasswordResetProperties(null, VALIDITY));
        assertThatIllegalArgumentException().isThrownBy(() -> new PasswordResetProperties(RESET_URL, Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new PasswordResetProperties(RESET_URL, null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PasswordResetProperties(RESET_URL, Duration.ofMinutes(-1)));
    }

    @Test
    void theRequestDoesNotPrintTheTokenOrThePassword() {
        assertThat(new PasswordResetConfirmRequest(TOKEN, NEW_PASSWORD).toString())
                .doesNotContain(TOKEN).doesNotContain(NEW_PASSWORD);
    }

    private void assertInvalidToken() {
        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest(TOKEN, NEW_PASSWORD)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESET_TOKEN);
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                });
    }

    private PasswordResetToken storedToken(User user, Instant expiresAt, Instant usedAt) {
        PasswordResetToken token = new PasswordResetToken(user, PasswordResetService.hash(TOKEN), expiresAt,
                NOW.minus(VALIDITY));
        if (usedAt != null) {
            token.markUsed(usedAt);
        }
        when(tokenRepository.findLockedByTokenHash(PasswordResetService.hash(TOKEN))).thenReturn(Optional.of(token));
        return token;
    }

    private User account(EntityStatus status) {
        User user = new User(new Role(RoleName.GUARDIAN, null), "ana@teensport.test",
                passwordEncoder.encode("Anterior#2026"), Instant.parse("2026-10-01T00:00:00Z"));
        user.setFirstName("Ana");
        user.setLastName("Gómez");
        user.setStatus(status);
        ReflectionTestUtils.setField(user, "id", 10);
        return user;
    }
}
