package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unicidad de cuentas en {@link AccountCreator} cuando la base rechaza una escritura simultánea
 * que superó la comprobación previa (RNF-11).
 */
class AccountCreatorTest {

    private static final Instant NOW = Instant.parse("2026-11-09T15:00:00Z");
    private static final String EMAIL = "ana@teensport.test";
    private static final String ID_NUMBER = "123456789";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final DataIntegrityViolationException uniqueViolation =
            new DataIntegrityViolationException("duplicate key value violates unique constraint");
    private AccountCreator accountCreator;

    @BeforeEach
    void setUp() {
        accountCreator = new AccountCreator(userRepository, roleRepository, new BCryptPasswordEncoder(4),
                new PasswordPolicy(), new AccountConflictLookup(userRepository), Clock.fixed(NOW, ZoneOffset.UTC));
        when(roleRepository.findByName(RoleName.GUARDIAN))
                .thenReturn(Optional.of(new Role(RoleName.GUARDIAN, "Acudiente")));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAndFlushesTheAccountSoUniquenessIsCheckedImmediately() {
        User user = accountCreator.create(account());

        assertThat(user.getEmail()).isEqualTo(EMAIL);
        verify(userRepository).save(user);
        verify(userRepository).flush();
    }

    @Test
    void concurrentEmailRejectedByTheDatabaseIsAnEmailConflict() {
        when(userRepository.save(any())).thenThrow(uniqueViolation);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false, true);

        assertThatThrownBy(() -> accountCreator.create(account()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getMessage()).isEqualTo(AccountCreator.EMAIL_TAKEN_MESSAGE);
                    assertThat(ex.getCause()).isSameAs(uniqueViolation);
                });
    }

    @Test
    void concurrentDocumentRejectedAtFlushIsADocumentConflict() {
        doThrow(uniqueViolation).when(userRepository).flush();
        when(userRepository.existsByDocumentNumber(ID_NUMBER)).thenReturn(false, true);

        assertThatThrownBy(() -> accountCreator.create(account()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.ID_NUMBER_ALREADY_REGISTERED);
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getMessage()).isEqualTo(AccountCreator.ID_NUMBER_TAKEN_MESSAGE);
                });
    }

    @Test
    void integrityViolationNotCausedByEmailOrDocumentIsRethrownUnchanged() {
        when(userRepository.save(any())).thenThrow(uniqueViolation);

        assertThatThrownBy(() -> accountCreator.create(account())).isSameAs(uniqueViolation);
    }

    @Test
    void changesAndFlushesTheEmailOfAnExistingAccount() {
        User user = existingUser();

        accountCreator.changeEmail(user, "nuevo@teensport.test");

        assertThat(user.getEmail()).isEqualTo("nuevo@teensport.test");
        verify(userRepository).flush();
    }

    @Test
    void emailAlreadyRegisteredIsRejectedBeforeChangingIt() {
        User user = existingUser();
        when(userRepository.existsByEmail("nuevo@teensport.test")).thenReturn(true);

        assertThatThrownBy(() -> accountCreator.changeEmail(user, "nuevo@teensport.test"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED));
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        verify(userRepository, never()).flush();
    }

    @Test
    void concurrentEmailChangeRejectedByTheDatabaseIsAnEmailConflict() {
        User user = existingUser();
        doThrow(uniqueViolation).when(userRepository).flush();
        when(userRepository.existsByEmail("nuevo@teensport.test")).thenReturn(false, true);

        assertThatThrownBy(() -> accountCreator.changeEmail(user, "nuevo@teensport.test"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED));
        verify(userRepository, never()).existsByDocumentNumber(anyString());
    }

    private static AccountCreator.NewAccount account() {
        return new AccountCreator.NewAccount(RoleName.GUARDIAN, "CC", ID_NUMBER, "Ana", "Pérez", EMAIL,
                "3001234567", "Segura#2026");
    }

    private static User existingUser() {
        return new User(new Role(RoleName.GUARDIAN, "Acudiente"), EMAIL, "$2a$hash", NOW);
    }
}
