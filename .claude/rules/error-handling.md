# Manejo de errores

- Centralizar el manejo de errores HTTP en un componente como `GlobalExceptionHandler`, usando `@RestControllerAdvice`.
- Definir excepciones específicas para situaciones de negocio relevantes.
- Diferenciar validación incorrecta, autenticación fallida, acceso prohibido, recurso inexistente, conflicto y error interno.
- Utilizar códigos HTTP coherentes.
- Devolver un contrato de error uniforme.
- No capturar excepciones para ignorarlas.
- No devolver respuestas exitosas cuando una operación ha fallado.
- Registrar información técnica útil sin revelar secretos ni datos personales.
- No devolver stack traces, consultas SQL ni detalles internos al cliente.
- Preservar la causa original de los errores cuando sea necesario para el diagnóstico.
- Probar las rutas de error y sus respuestas.
- Si una operación transaccional falla, comprobar que no deje cambios parciales cuando deba garantizarse atomicidad.