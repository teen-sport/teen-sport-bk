package com.uniquindio.teen_sport.config;

import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.repository.RoleRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.EmailNormalizer;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.time.Clock;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AdminBootstrapProperties adminProperties;
    private final Clock clock;

    /**
     * @param roleRepository  catálogo de roles
     * @param userRepository  cuentas
     * @param passwordEncoder hash BCrypt
     * @param passwordPolicy  fortaleza de contraseñas
     * @param adminProperties datos del administrador inicial
     * @param clock           reloj de la aplicación
     */
    public ReferenceDataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                                    PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
                                    AdminBootstrapProperties adminProperties, Clock clock) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.adminProperties = adminProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleName name : RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name, ROLE_DESCRIPTIONS.get(name)));
            }
        }
        if (adminProperties.isConfigured() && !userRepository.existsByRoleName(RoleName.ADMIN)) {
            createInitialAdmin();
        }
    }

    private void createInitialAdmin() {
        passwordPolicy.validate(adminProperties.password());
        String email = EmailNormalizer.normalize(adminProperties.email());
        String idNumber = adminProperties.idNumber().trim();
        if (userRepository.existsByEmail(email) || userRepository.existsByDocumentNumber(idNumber)) {
            throw new IllegalStateException(
                    "El correo o el documento del administrador inicial ya pertenecen a otra cuenta");
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User admin = new User(adminRole, email, passwordEncoder.encode(adminProperties.password()), clock.instant());
        admin.setDocumentType(adminProperties.idType().trim());
        admin.setDocumentNumber(idNumber);
        admin.setFirstName(adminProperties.firstNames().trim());
        admin.setLastName(adminProperties.lastNames().trim());
        userRepository.save(admin);
        LOGGER.info("Se creó el administrador inicial");
    }
}
