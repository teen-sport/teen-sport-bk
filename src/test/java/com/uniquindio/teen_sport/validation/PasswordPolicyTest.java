package com.uniquindio.teen_sport.validation;

import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"Segura#26", "aB3$efgh", "Ññandú-2026", "Contraseña_Larga_Con_1_Simbolo"})
    void acceptsPasswordsWithUpperLowerDigitAndSymbol(String password) {
        assertThatCode(() -> policy.validate(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "segura#2026",   // sin mayúscula
            "SEGURA#2026",   // sin minúscula
            "Segura#Clave",  // sin número
            "Segura2026",    // sin símbolo
            "Segura 2026",   // el espacio no cuenta como símbolo
            "Se#1",          // demasiado corta
    })
    void rejectsWeakPasswordsWithWeakPasswordCode(String password) {
        assertThatThrownBy(() -> policy.validate(password))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.WEAK_PASSWORD);
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                });
    }

    @Test
    void rejectsNullPassword() {
        assertThatThrownBy(() -> policy.validate(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsPasswordAboveBcryptByteLimitEvenWithFewCharacters() {
        String multibyte = "Ñ".repeat(37) + "a1#"; // 40 caracteres, 77 bytes en UTF-8
        assertThat(PasswordPolicy.exceedsMaxBytes(multibyte)).isTrue();
        assertThatThrownBy(() -> policy.validate(multibyte)).isInstanceOf(BusinessException.class);
    }

    @Test
    void acceptsPasswordOfExactlyMaxLength() {
        String password = "Aa1#" + "x".repeat(PasswordPolicy.MAX_LENGTH - 4);
        assertThat(PasswordPolicy.exceedsMaxBytes(password)).isFalse();
        assertThatCode(() -> policy.validate(password)).doesNotThrowAnyException();
    }
}
