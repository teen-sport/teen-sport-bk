# 10. Plan de implementación incremental

El orden busca construir primero las capacidades de las que dependen los módulos de negocio. Cada etapa debe cerrar con compilación, pruebas y documentación actualizada antes de avanzar.

## Fase 0 — Preparación del repositorio
- Confirmar versión de Java/Spring Boot, sistema de construcción y motor relacional.
- Mantener una única configuración general segura, sin separación de ambientes.
- Añadir formato de error, validación, OpenAPI y pruebas base.
- Validar el contrato OpenAPI y decidir cuál archivo saneado será fuente de verdad.

**Estado (2026-10-11): completada.**
- Hecho: Java 21 / Spring Boot 4.1.1 / Gradle confirmados; motor PostgreSQL 18.6. Paquete `com.uniquindio.teen_sport` conservado; prefijo `/api/v1` en `config/WebConfig`; `HelloController` eliminado. Dependencias `spring-boot-starter-validation` y `springdoc-openapi-starter-webmvc-ui` 3.1.1. Un único ambiente general en `application.properties` (decisión: sin separación de ambientes ni archivos `-dev`/`-prod`); mensajes de validación en español. Formato `Error`, `ErrorCode`, `BusinessException` y `GlobalExceptionHandler` (400, 404, errores de negocio y 500 `INTERNAL_ERROR` para fallos no previstos; las excepciones de Spring MVC con estado propio, como 405 o 415, conservan su estado).
- Decisión: sin migraciones versionadas por ahora; las entidades JPA mapean el MER. Desde 2026-10-12, por defecto la aplicación solo valida el esquema (`DB_DDL_AUTO=validate`) y no modifica la estructura de la base; `update` queda para bases locales.
- Contrato: `UNAUTHENTICATED` y `TOKEN_EXPIRED` (401) ya figuraban en `openapi.yaml`; se añadió `INTERNAL_ERROR` (500) y los tres se documentaron en `docs/07`.
- Persistencia: `spring-boot-starter-data-jpa` y driver `org.postgresql:postgresql` (versiones gestionadas por Spring Boot 4.1.1); conexión por variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), `ddl-auto` configurable con `DB_DDL_AUTO` (por defecto `validate`) y `open-in-view=false`.
- Pruebas: Testcontainers (`postgres:18.6`) para las pruebas de contexto completo; nuevas pruebas del 500 sin detalles internos y de 405/415.
- `ErrorCode` incluye `UNAUTHENTICATED`, `TOKEN_EXPIRED` e `INTERNAL_ERROR`.
- Pendiente para la Fase 1: las excepciones de Spring Security (401/403) deben manejarse explícitamente para que no terminen en el manejador de 500.

## Fase 1 — Seguridad y cuentas
- Entidades JPA para roles, users, guardians y coaches según MER.
- Registro de acudiente y login.
- BCrypt, JWT (resource server de Spring Security), contexto autenticado y manejo global de errores.
- Pruebas de roles, token expirado y propiedad de recursos.

Se desarrolla por historias de usuario, una rama por HU: HU28 (U1, U2), HU29 (U3–U5) y HU30 (U6–U12).

**Estado (2026-10-11): en curso.**
- HU28 integrada en `development` (PR #11): entidades `roles`, `users`, `guardians`, `coaches`; JWT HS256 con resource server; BCrypt; política de contraseñas; registro de acudiente (U2) y login (U1); 401/403 con formato `Error`; CORS explícito; roles y administrador inicial al arrancar. `./gradlew check`: 82 pruebas en verde.
- Limitación conocida: dos registros simultáneos con el mismo correo o documento no duplican datos (restricción única en PostgreSQL), pero la segunda solicitud recibe 500 en lugar de 409.
- HU29 integrada en `development` (PR #12): U3 `GET /users/me`, U4 `PUT /users/me` (reemplazo completo; campos de otro rol ignorados), U5 `PUT /users/me/password`; identidad tomada del JWT (`CurrentUserService`); `RoleProfileResolver` compartido con el login. `./gradlew check`: 109 pruebas en verde.
- HU30 implementada en `feature/HU30/user-administration` (pendiente de revisión y PR): U6 listado paginado con filtros y búsqueda, U7 detalle, U8 creación de `ADMIN`/`COACH`, U9 edición, U10 activación/desactivación, U11 entrenadores, U12 acudientes; autorización `ADMIN` en el filtro y con `@PreAuthorize`; carga en bloque de perfiles para evitar N+1; `AccountCreator` y `ProfileEditor` compartidos con U2, U4 y el administrador inicial. `./gradlew check`: 147 pruebas en verde.
- `COACH_HAS_ACTIVE_GROUPS` en U10: cerrado en HU33.

## Fase 2 — Deportistas y catálogos
- Perfiles de deportistas.
- Disciplinas y reglas de estado.
- DTO, validaciones y filtros de consulta.

Se desarrolla por historias de usuario: HU32 (H1–H5, disciplinas) y HU34 (E1–E4, deportistas).

**Estado (2026-10-12): en curso.**
- HU32 integrada en `development` (PR #17): entidad `disciplines`, H1–H5, nombre único sin distinguir mayúsculas, mensualidad `DECIMAL(10,2)` > 0, listado de solo activas para no administradores, escritura solo `ADMIN`. `./gradlew check`: 164 pruebas en verde.
- Decisiones para HU34: edad calculada con la zona de la academia (`America/Bogota`, configurable); en E4 la edad solo se valida si cambia `birthDate`; un administrador no puede registrar deportistas para un acudiente inactivo.
- HU34 implementada en `feature/HU34/athlete-management` (pendiente de revisión y PR): entidad `athletes`; E1–E4 con propiedad por acudiente (`AthleteAccessPolicy`), edad 12–17 con la fecha local de la academia (`AcademyCalendar`, `APP_TIME_ZONE`), documento único, acudiente activo obligatorio para el administrador. `./gradlew check`: 196 pruebas en verde.
- `DISCIPLINE_HAS_ACTIVE_GROUPS` en H5: cerrado en HU33.
- Pendiente para la Fase 4: calcular `hasActiveEnrollment` de los deportistas con las matrículas.

## Fase 3 — Grupos y horarios
- Grupos y asignación de entrenador.
- Capacidad y validación de entrenador/disciplina activos.
- Horarios semanales.

Se desarrolla en una sola historia de usuario: HU33 (H6–H14).

**Estado (2026-10-13): implementada, pendiente de revisión.**
- HU33 en `feature/HU33/group-schedule-management`: entidades `groups` (nombre físico decidido el 2026-10-13) y `schedules`; H6–H14; propiedad de grupos para entrenadores (`GroupAccessPolicy`); detección de cruces (`ScheduleConflictChecker`, solo grupos activos, franjas contiguas permitidas) con bloqueo pesimista del grupo y del entrenador; reactivar un grupo (H10) comprueba cruces con los demás grupos activos del entrenador (`COACH_SCHEDULE_CONFLICT`, añadido al contrato); cierre de `COACH_HAS_ACTIVE_GROUPS` (U10) y `DISCIPLINE_HAS_ACTIVE_GROUPS` (H5). Mockito pasa a cargarse como `-javaagent`. `./gradlew check`: 245 pruebas en verde.
- Pendiente para la Fase 4: `CAPACITY_BELOW_OCCUPIED` (H9) y matrículas vigentes en `GROUP_HAS_ACTIVITY` (H10).
- Pendiente para la Fase 6: sesiones futuras en `GROUP_HAS_ACTIVITY` (H10) y traspaso de sesiones futuras al nuevo entrenador (H11).

## Fase 4 — Matrículas y disponibilidad
- Disponibilidad por periodo.
- Solicitud de matrícula y control de cupos bajo concurrencia.
- Confirmación, rechazo, cancelación y cambio de grupo.
- Pruebas de transacciones y consistencia.

## Fase 5 — Pagos presenciales
- Reporte por acudiente.
- Registro por administrador e historial.
- Estados de pago conforme al contrato; sin pasarela ni job automático.

## Fase 6 — Sesiones y asistencia
- Creación/generación de sesiones.
- Lista elegible y registro de asistencia.
- Consultas históricas filtradas por rol.

## Fase 7 — Informes y correo
- Informes R1–R4 con consultas probadas.
- Adaptador Brevo desacoplado y notificaciones posteriores al commit.
- Observabilidad y pruebas de fallos del proveedor.

## Fase 8 — Funcionalidades pendientes de contrato
- Definir API de torneos antes de implementarla, ya que el MER contiene entidades no expuestas por OpenAPI.
- Definir API y ciclo de vida de notificaciones si requiere consultas desde el frontend.

## Fase 9 — Calidad y despliegue
- Ejecutar suite completa, revisar cobertura y Javadoc.
- Medir RNF de rendimiento y seguridad.
- Validar CORS, secretos y configuración general.
- Preparar pipeline Jenkins y despliegue Azure cuando estén aprobados.
