package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Política de fortaleza de contraseñas aprobada para TeenSport.
 *
 * <p>Una contraseña válida tiene entre {@value #MIN_LENGTH} y {@value #MAX_LENGTH} caracteres
 * (y no más de {@value #MAX_LENGTH} bytes en UTF-8, límite de BCrypt) e incluye al menos una
 * mayúscula, una minúscula, un número y un símbolo. Los DTO validan la longitud con Bean
 * Validation (400); esta clase valida la composición y responde 422 {@code WEAK_PASSWORD}.</p>
 */
@Component
public class PasswordPolicy {

    /** Longitud mínima en caracteres. */
    public static final int MIN_LENGTH = 8;

    /** Longitud máxima en caracteres y en bytes UTF-8 (límite de BCrypt). */
    public static final int MAX_LENGTH = 72;

    static final String WEAK_PASSWORD_MESSAGE = "La contraseña debe tener entre 8 y 72 caracteres e incluir "
            + "al menos una mayúscula, una minúscula, un número y un símbolo.";

    /**
     * @param password contraseña en claro
     * @throws BusinessException 422 {@code WEAK_PASSWORD} si no cumple la política
     */
    public void validate(String password) {
        if (!isStrong(password)) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD, HttpStatus.UNPROCESSABLE_CONTENT,
                    WEAK_PASSWORD_MESSAGE);
        }
    }

    /**
     * @param password contraseña en claro
     * @return {@code true} si supera el límite de bytes que BCrypt puede procesar
     */
    public static boolean exceedsMaxBytes(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH;
    }

    private static boolean isStrong(String password) {
        if (password == null || password.length() < MIN_LENGTH || exceedsMaxBytes(password)) {
            return false;
        }
        return password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(PasswordPolicy::isSymbol);
    }

    private static boolean isSymbol(int character) {
        return !Character.isLetterOrDigit(character) && !Character.isWhitespace(character);
    }
}
