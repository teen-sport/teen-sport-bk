package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Restricciones de integridad del MER aplicadas por PostgreSQL, no solo por los servicios.
 */
@IntegrationTest
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private GuardianRepository guardianRepository;

    @Test
    void emailIsUniqueInTheDatabase() {
        String email = TestAccounts.uniqueEmail();
        userRepository.saveAndFlush(user(email, TestAccounts.uniqueIdNumber()));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user(email, TestAccounts.uniqueIdNumber())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void documentNumberIsUniqueInTheDatabase() {
        String idNumber = TestAccounts.uniqueIdNumber();
        userRepository.saveAndFlush(user(TestAccounts.uniqueEmail(), idNumber));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user(TestAccounts.uniqueEmail(), idNumber)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aUserHasAtMostOneGuardianProfile() {
        User user = userRepository.saveAndFlush(user(TestAccounts.uniqueEmail(), TestAccounts.uniqueIdNumber()));
        guardianRepository.saveAndFlush(new Guardian(user, null));

        assertThatThrownBy(() -> guardianRepository.saveAndFlush(new Guardian(user, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsQueriesFindRegisteredValues() {
        String email = TestAccounts.uniqueEmail();
        String idNumber = TestAccounts.uniqueIdNumber();
        userRepository.saveAndFlush(user(email, idNumber));

        assertThat(userRepository.existsByEmail(email)).isTrue();
        assertThat(userRepository.existsByDocumentNumber(idNumber)).isTrue();
        assertThat(userRepository.existsByEmail(TestAccounts.uniqueEmail())).isFalse();
        assertThat(userRepository.existsByRoleName(RoleName.ADMIN)).isTrue();
    }

    private User user(String email, String idNumber) {
        Role guardian = roleRepository.findByName(RoleName.GUARDIAN).orElseThrow();
        User user = new User(guardian, email, "$2a$10$hashDePruebaNoUsableParaLogin", Instant.now());
        user.setDocumentType("CC");
        user.setDocumentNumber(idNumber);
        user.setFirstName("Persona");
        user.setLastName("De Prueba");
        return user;
    }
}
