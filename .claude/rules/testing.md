# Reglas de pruebas automatizadas

- Toda regla de negocio nueva debe contar con pruebas unitarias.
- Utilizar JUnit y Mockito para probar los servicios cuando corresponda.
- Utilizar pruebas de integración para verificar persistencia, transacciones y configuración de seguridad.
- Probar entradas válidas, inválidas y casos límite.
- Probar restricciones de integridad y operaciones concurrentes relevantes.
- Verificar los códigos HTTP y el formato de las respuestas de error.
- Probar autorización por rol y por propiedad del recurso.
- Probar el vencimiento de JWT con un reloj controlable o un mecanismo de prueba equivalente.
- Aislar los servicios externos mediante mocks o adaptadores de prueba.
- No depender de servicios reales de correo ni credenciales productivas para las pruebas automatizadas.
- Usar JaCoCo para medir la cobertura.
- No escribir pruebas que dependan del orden accidental de ejecución.
- No modificar o eliminar pruebas existentes solo para hacer pasar una implementación incorrecta.
- Ejecutar las pruebas relevantes antes de declarar una tarea terminada.

La cobertura es una métrica de apoyo: una prueba que ejecuta una línea no demuestra por sí sola que el comportamiento sea correcto.