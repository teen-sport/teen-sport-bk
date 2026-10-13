# 08. Estrategia de pruebas del backend

## Pirámide
1. Unitarias de servicios y reglas de negocio.
2. Pruebas de slice para controladores/validación y repositorios cuando aporten valor.
3. Pruebas de contrato y seguridad de endpoints.
4. Pruebas de rendimiento para RNF medibles.

## Herramientas sugeridas
JUnit 5, Mockito, Spring Boot Test, MockMvc o WebTestClient según el stack MVC/reactivo real, Testcontainers con `postgres:18.6` para las pruebas que levantan el contexto completo o tocan persistencia (`PostgresTestcontainersConfiguration`; requiere Docker en ejecución), JaCoCo para cobertura y validación automatizada del OpenAPI. Confirmar versiones con `build.gradle`/`pom.xml` antes de añadir dependencias.

## Casos transversales
- Validación de DTO y errores de entrada.
- Respuesta HTTP y estructura `Error`.
- Autenticación y autorización por rol.
- Autorización por propiedad del recurso.
- Consistencia transaccional ante fallos.
- Paginación y límites (`page >= 0`, `size` entre 1 y 100 según contrato).
- Fechas, periodos y límites de edad.
- Consultas vacías, filtros combinados y datos no encontrados.

## Casos de negocio prioritarios
### Matrículas y cupos
- Dos solicitudes concurrentes para el último cupo no deben superar `maxCapacity`.
- Una matrícula rechazada/cancelada debe liberar el cupo cuando la regla lo establece.
- Cambio de grupo libera cupo anterior y reserva el nuevo de forma atómica.
- No se permite una segunda matrícula vigente del mismo deportista en la misma disciplina (otro grupo u otro mes de inicio incluidos); sí en otra disciplina, y sí tras rechazo o cancelación.
- Dos solicitudes concurrentes del mismo deportista para la misma disciplina no deben crear dos matrículas vigentes.

### Pagos
- Reportar pago no cambia automáticamente `paymentStatus`.
- Solo el administrador registra pago.
- El importe debe cubrir `monthlyFee`.
- Una matrícula admite varios pagos, pero no dos con `paymentDate` en el mismo mes calendario (`PAYMENT_ALREADY_REGISTERED`).
- El pago actualiza el estado a `UP_TO_DATE` y limpia `paymentReported`.

### Sesiones y asistencia
- No registrar asistencia para sesión futura o cancelada.
- El entrenador debe estar asignado al grupo.
- La lista de asistencia debe corresponder a matrículas `ACTIVE` con mes de inicio igual o anterior al mes de la sesión.
- Guardar de nuevo debe reemplazar marcas previas de forma controlada.

### JWT y permisos
- Todos los endpoints protegidos rechazan token ausente/inválido/expirado.
- Cada rol solo puede invocar las operaciones permitidas.
- Las listas aplican filtrado de pertenencia en el servidor.

## Trazabilidad y evidencia
Cada prueba importante debe relacionarse con endpoint ID, requisito funcional y RNF aplicable. Registrar comando, entorno, resultado y limitaciones. No declarar que una métrica cumple sin una medición reproducible.
