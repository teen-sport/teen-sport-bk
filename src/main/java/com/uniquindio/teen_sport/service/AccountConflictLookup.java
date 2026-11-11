package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Identifica qué dato único de una cuenta (correo o documento) ya quedó registrado después de que
 * la base de datos rechazó una escritura por una restricción única.
 *
 * <p>Ese rechazo ocurre cuando dos solicitudes simultáneas superan la comprobación previa de
 * {@link AccountCreator} antes de que la otra confirme su escritura. Se consulta en una transacción
 * nueva porque PostgreSQL aborta la transacción que recibió el error y no admite más consultas en
 * ella; en ese momento la escritura que ganó ya está confirmada y es visible. Así la traducción no
 * depende de los nombres de las restricciones en la base desplegada, que se crea fuera de la
 * aplicación.</p>
 */
@Component
public class AccountConflictLookup {

    private final UserRepository userRepository;

    /**
     * @param userRepository cuentas
     */
    public AccountConflictLookup(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @param email          correo normalizado que se intentó guardar
     * @param documentNumber documento que se intentó guardar, o {@code null} si no cambió
     * @return el código del dato ya registrado; vacío si ninguno lo está y el rechazo tuvo otra causa
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ErrorCode> find(String email, String documentNumber) {
        if (userRepository.existsByEmail(email)) {
            return Optional.of(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        if (documentNumber != null && userRepository.existsByDocumentNumber(documentNumber)) {
            return Optional.of(ErrorCode.ID_NUMBER_ALREADY_REGISTERED);
        }
        return Optional.empty();
    }
}
