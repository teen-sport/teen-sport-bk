package com.uniquindio.teen_sport.config;

import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.support.IntegrationTest;
import com.uniquindio.teen_sport.support.TestAccounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Datos de referencia creados al arrancar: roles y administrador inicial, sin duplicados.
 */
@IntegrationTest
class ReferenceDataInitializerIntegrationTest {

    @Autowired
    private ReferenceDataInitializer initializer;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void createsTheThreeRolesAndTheInitialAdminOnlyOnce() {
        initializer.run(new DefaultApplicationArguments());
        initializer.run(new DefaultApplicationArguments());

        assertThat(roleRepository.findAll()).extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.COACH, RoleName.GUARDIAN);
        assertThat(userRepository.findAll()).filteredOn(user -> TestAccounts.ADMIN_EMAIL.equals(user.getEmail()))
                .singleElement()
                .satisfies(admin -> {
                    assertThat(admin.getDocumentNumber()).isEqualTo(TestAccounts.ADMIN_ID_NUMBER);
                    assertThat(admin.getPasswordHash()).isNotEqualTo(TestAccounts.ADMIN_PASSWORD);
                    assertThat(admin.isActive()).isTrue();
                });
        assertThat(userRepository.findByEmail(TestAccounts.ADMIN_EMAIL)).map(User::getRole).map(Role::getName)
                .contains(RoleName.ADMIN);
    }
}
