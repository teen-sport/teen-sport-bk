package com.uniquindio.teen_sport.exception;

/**
 * Campo de entrada inválido que solo puede comprobarse en el caso de uso (por ejemplo, porque
 * depende del rol de quien llama o de datos guardados).
 *
 * <p>Se responde igual que un error de Bean Validation: 400 {@code VALIDATION_ERROR} con el campo
 * en {@code details}, para que el cliente lo trate de la misma forma.</p>
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    /**
     * @param field   nombre del campo en el contrato
     * @param message motivo, en español
     */
    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * @return nombre del campo rechazado
     */
    public String getField() {
        return field;
    }
}
