package com.uniquindio.teen_sport.config;

import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.service.AccountCreator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de referencia que la aplicación necesita para operar, creados al arrancar.
 *
 * <p>Como no hay migraciones versionadas, este componente garantiza que existan los tres roles
 * del contrato y, si se configuró, el administrador inicial (único modo de obtener la primera
 * cuenta {@code ADMIN}, ya que U8 exige un administrador). Es idempotente: no duplica roles ni
 * crea un segundo administrador.</p>
 */
@Component
public class ReferenceDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataInitializer.class);

    private static final Map<RoleName, String> ROLE_DESCRIPTIONS = Map.of(
            RoleName.ADMIN, "Administrador de la academia",
            RoleName.COACH, "Entrenador",
            RoleName.GUARDIAN, "Acudiente de deportistas");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AccountCreator accountCreator;
    private final AdminBootstrapProperties adminProperties;

    /**
     * @param roleRepository  catálogo de roles
     * @param userRepository  cuentas
     * @param accountCreator  creación de cuentas con las reglas comunes
     * @param adminProperties datos del administrador inicial
     */
    public ReferenceDataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                                    AccountCreator accountCreator, AdminBootstrapProperties adminProperties) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.accountCreator = accountCreator;
        this.adminProperties = adminProperties;
    }

    /**
     * Crea los roles que falten y, si corresponde, el administrador inicial. Si los datos del
     * administrador no cumplen las reglas de cuentas (contraseña débil, correo o documento ya
     * usados), la aplicación no arranca.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleName name : RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name, ROLE_DESCRIPTIONS.get(name)));
            }
        }
        if (adminProperties.isConfigured() && !userRepository.existsByRoleName(RoleName.ADMIN)) {
            accountCreator.create(new AccountCreator.NewAccount(RoleName.ADMIN, adminProperties.idType(),
                    adminProperties.idNumber(), adminProperties.firstNames(), adminProperties.lastNames(),
                    adminProperties.email(), null, adminProperties.password()));
            LOGGER.info("Se creó el administrador inicial");
        }
    }
}
