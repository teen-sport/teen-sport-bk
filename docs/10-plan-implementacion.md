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

**Estado (2026-10-13): integrada en `development` (PR #19).**
- HU33: entidades `groups` (nombre físico decidido el 2026-10-13) y `schedules`; H6–H14; propiedad de grupos para entrenadores (`GroupAccessPolicy`); detección de cruces (`ScheduleConflictChecker`, solo grupos activos, franjas contiguas permitidas) con bloqueo pesimista del grupo y del entrenador; reactivar un grupo (H10) comprueba cruces con los demás grupos activos del entrenador (`COACH_SCHEDULE_CONFLICT`, añadido al contrato); cierre de `COACH_HAS_ACTIVE_GROUPS` (U10) y `DISCIPLINE_HAS_ACTIVE_GROUPS` (H5). Mockito pasa a cargarse como `-javaagent`. `./gradlew check`: 245 pruebas en verde.
- `CAPACITY_BELOW_OCCUPIED` (H9) y matrículas vigentes en `GROUP_HAS_ACTIVITY` (H10): cerrados en HU35.
- Pendiente para la Fase 6: sesiones futuras en `GROUP_HAS_ACTIVITY` (H10) y traspaso de sesiones futuras al nuevo entrenador (H11).

## Fase 4 — Matrículas y disponibilidad
- Disponibilidad por periodo.
- Solicitud de matrícula y control de cupos bajo concurrencia.
- Confirmación, rechazo, cancelación y cambio de grupo.
- Pruebas de transacciones y consistencia.

Se desarrolla en dos historias de usuario: HU35 Solicitud de matrícula (E5–E8, más los cierres de H9, H10 y `hasActiveEnrollment`) y HU37 Revisión y ciclo de la matrícula (E12–E15).

Decisiones (2026-10-13):
- Cupos: una matrícula vigente ocupa cupo desde su mes de inicio en adelante, así que E6, H9 (y E12/E15 en HU37) validan contra todas las matrículas vigentes del grupo. En E5, `occupiedSpots` cuenta las que empezaron en el periodo o antes y `availableSpots` es la capacidad menos todas las vigentes (mínimo 0).
- Filtro `period` de E7: matrículas vigentes en ese mes (inicio igual o anterior y, sin filtro de estado, `PENDING_REVIEW` o `ACTIVE`).
- El motivo opcional de E14 se guarda en `review_notes` (HU37).
- Correos (`NO_SPOTS_AVAILABLE`, `ENROLLMENT_CONFIRMED`, `ENROLLMENT_REJECTED`): pendientes de la Fase 7. Pago dentro de E12: cerrado en HU36 (Fase 5).

**Estado (2026-10-14): HU35 (PR #20) y HU37 (PR #21) integradas en `development`.**
- HU35: entidad `enrollments` (sin las columnas de soportes documentales); E5 `GET /availability`, E6 `POST /enrollments`, E7 `GET /enrollments`, E8 `GET /enrollments/{id}`; cupos en `SpotCounter` (una consulta agrupada para E5); E6 bloquea el deportista y luego el grupo antes de comprobar duplicado y cupo; H9 `CAPACITY_BELOW_OCCUPIED` y H10 `GROUP_HAS_ACTIVITY` (matrículas) con bloqueo del grupo; `hasActiveEnrollment` real en E1–E4 (en lote en E2). Los mappers de grupos y matrículas pasan a ser funciones estáticas. `./gradlew check`: 287 pruebas en verde.
- HU37: E12 `confirm`, E13 `reject`, E14 `cancel`, E15 `group` en `EnrollmentLifecycleService`; cada operación bloquea la matrícula y, al cambiar de grupo, el grupo de destino antes de contar cupos; reglas de grupo compartidas con E6 en `GroupEnrollmentPolicy`; reglas por URL para que solo `ADMIN` revise o mueva matrículas. `./gradlew check`: 310 pruebas en verde.
- Pendiente: correos `NO_SPOTS_AVAILABLE`, `ENROLLMENT_CONFIRMED` y `ENROLLMENT_REJECTED` (Fase 7).

## Fase 5 — Pagos presenciales
- Reporte por acudiente.
- Registro por administrador e historial.
- Estados de pago conforme al contrato; sin pasarela ni job automático.

Se desarrolla en una historia de usuario: HU36 Gestión de pagos (E16–E19 y el pago dentro de E12).

Decisiones (2026-10-14):
- E16: la fecha que indica el acudiente solo sirve para detectar un mes ya pagado; `payment_reported_at` guarda el momento del reporte.
- `receiptReference` se limita a 50 caracteres, como el contrato, aunque la columna del MER admite 100.
- E18 se admite en cualquier estado de la matrícula y no cambia nada si ya estaba `OVERDUE`.
- Un pago puede tener fecha anterior al mes de inicio de la matrícula; no hay regla que lo impida.

**Estado (2026-10-14): HU36 implementada, pendiente de revisión.**
- HU36 en `feature/HU36/payment-management`: entidad `payment_history`; E16 `payment-report`, E17 `payments`, E18 `payment-status` en `PaymentService`; E19 `GET /payments` en `PaymentQueryService`; reglas del pago (monto mínimo, fecha no futura, un pago por mes) en `PaymentRecorder`, compartido con E12; estados admitidos por operación en `EnrollmentStatePolicy`. E12, E16, E17 y E18 bloquean la matrícula, así que dos registros simultáneos del mismo mes guardan solo uno. Reglas por URL: registrar y marcar mora solo `ADMIN`, reportar solo `GUARDIAN`. `./gradlew check`: 336 pruebas en verde.

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
