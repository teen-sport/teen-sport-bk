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
- ~~Limitación conocida: dos registros simultáneos con el mismo correo o documento no duplican datos (restricción única en PostgreSQL), pero la segunda solicitud recibe 500 en lugar de 409.~~ Resuelta el 2026-11-09 (Entrega 3, etapa 2).
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
- Sesiones futuras en `GROUP_HAS_ACTIVITY` (H10) y su traspaso al nuevo entrenador (H11): cerrados en HU39 (Fase 6).

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
- Correos (`NO_SPOTS_AVAILABLE`, `ENROLLMENT_CONFIRMED`, `ENROLLMENT_REJECTED`): pendientes de la Entrega 3, etapa 6. Pago dentro de E12: cerrado en HU36 (Fase 5).

**Estado (2026-10-14): HU35 (PR #20) y HU37 (PR #21) integradas en `development`.**
- HU35: entidad `enrollments` (sin las columnas de soportes documentales); E5 `GET /availability`, E6 `POST /enrollments`, E7 `GET /enrollments`, E8 `GET /enrollments/{id}`; cupos en `SpotCounter` (una consulta agrupada para E5); E6 bloquea el deportista y luego el grupo antes de comprobar duplicado y cupo; H9 `CAPACITY_BELOW_OCCUPIED` y H10 `GROUP_HAS_ACTIVITY` (matrículas) con bloqueo del grupo; `hasActiveEnrollment` real en E1–E4 (en lote en E2). Los mappers de grupos y matrículas pasan a ser funciones estáticas. `./gradlew check`: 287 pruebas en verde.
- HU37: E12 `confirm`, E13 `reject`, E14 `cancel`, E15 `group` en `EnrollmentLifecycleService`; cada operación bloquea la matrícula y, al cambiar de grupo, el grupo de destino antes de contar cupos; reglas de grupo compartidas con E6 en `GroupEnrollmentPolicy`; reglas por URL para que solo `ADMIN` revise o mueva matrículas. `./gradlew check`: 310 pruebas en verde.
- Pendiente: correos `NO_SPOTS_AVAILABLE`, `ENROLLMENT_CONFIRMED` y `ENROLLMENT_REJECTED` (Entrega 3, etapa 6).

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

**Estado (2026-10-14): HU36 integrada en `development` (PR #22).**
- HU36: entidad `payment_history`; E16 `payment-report`, E17 `payments`, E18 `payment-status` en `PaymentService`; E19 `GET /payments` en `PaymentQueryService`; reglas del pago (monto mínimo, fecha no futura, un pago por mes) en `PaymentRecorder`, compartido con E12; estados admitidos por operación en `EnrollmentStatePolicy`. E12, E16, E17 y E18 bloquean la matrícula, así que dos registros simultáneos del mismo mes guardan solo uno. Reglas por URL: registrar y marcar mora solo `ADMIN`, reportar solo `GUARDIAN`. `./gradlew check`: 336 pruebas en verde.

## Fase 6 — Sesiones y asistencia
- Creación/generación de sesiones.
- Lista elegible y registro de asistencia.
- Consultas históricas filtradas por rol.

Se desarrolla en una historia de usuario: HU39 Registro y consulta de asistencia (A1–A9 y las sesiones futuras de H10 y H11).

Decisiones (2026-10-14):
- Una fecha "pasada" (A1, A2, A5) se compara solo por día en la zona de la academia: hoy se admite aunque la hora ya pasó.
- Una sesión "futura" en A8 es la de fecha posterior a hoy; la del mismo día se puede registrar.
- El entrenador que gestiona las sesiones y registra la asistencia es el actual del grupo.
- A3 sin fechas usa la semana actual (lunes a domingo); con un solo extremo, siete días. A2 y A3 abarcan como máximo 62 días, contando ambos extremos.
- En H10 y H11, "futuras" son las sesiones `SCHEDULED` con fecha de hoy en adelante.
- Las marcas de deportistas que salen de la lista se conservan en el historial (A9) y no aparecen en A7.

**Estado (2026-11-09): HU39 integrada en `development` (PR #23); `./gradlew check` completo en verde sobre `development` el 2026-11-09: 369 pruebas, 0 fallos (Entrega 3, etapa 1).**
- HU39: entidades `training_sessions` y `attendance_records`; A1, A2, A5 y A6 en `SessionService` (reglas de fecha, hora y duplicado en `SessionSlotPolicy`, rangos en `DateRangePolicy`); A3 y A4 en `SessionQueryService`; A7 y A8 en `AttendanceService`; A9 en `AttendanceQueryService`. Crear, generar y reprogramar bloquean el grupo; reprogramar, cancelar y registrar asistencia bloquean la sesión, así que no se duplican sesiones ni marcas. H10 y H11 consultan y traspasan las sesiones futuras mediante `GroupActivity`. Reglas por URL: registrar asistencia solo `COACH`.
- Pendiente: correos `SCHEDULE_PUBLISHED` (A2 con `notify`) y `SCHEDULE_CHANGE` (A5, A6), en la Entrega 3, etapa 6.

## Fase 7 — Alineación con los documentos oficiales (v02 y v03)
Origen: la Definición del Proyecto y el Plan de Calidad v02 (19/10/2026) reemplazaron el proceso de torneos por el de registro, autenticación y gestión de usuarios. La Definición y el SAD v03 (9/11/2026) añadieron la nota de alcance: el correo y las notificaciones (Brevo), la recuperación de contraseña, la creación de administradores desde la interfaz y la generación de reportes se planificaron para la tercera entrega. Los RNF del Plan de Calidad no cambiaron.

**Estado (2026-11-09): documentación interna alineada con la Definición v03, el SAD v03 y el Plan de Calidad v02.**
- Hecho (2026-10-19): actualización de `CLAUDE.md`, `docs/01`–`docs/10`, ADR-010 interno y skills de `.claude/` (alcance por procesos, exclusión de torneos, RNF v02, despliegue en GitHub Actions y Railway).
- Hecho (2026-11-09): versiones v03 en las fuentes de verdad, decisiones de alcance y reorganización del trabajo restante en la Entrega 3.

Decisiones del 2026-11-09:
- La documentación de la matrícula queda fuera del alcance; `E9–E11` siguen reservados.
- Se mantiene la matrícula continua mes a mes; no hay renovación explícita.
- La autorización del acudiente en U2 cubre a todos sus deportistas, también los que registre el administrador.
- El entrenador ve en A7 la edad del deportista y el contacto del acudiente.
- El correo, las notificaciones y la recuperación de contraseña se hacen al final de la Entrega 3; el catálogo de notificaciones se define en ese momento.
- La creación de administradores ya existe en la API (U8); la nota de alcance se refiere a la interfaz.

## Entrega 3 — Trabajo restante para cerrar el proyecto
Estamos en la tercera entrega (confirmado el 2026-11-09). El orden prioriza lo indispensable y deja el correo y la recuperación de contraseña para el final. Cada etapa se desarrolla en su propia rama (una HU por rama) y cierra con `./gradlew check`, documentación y trazabilidad actualizadas.

| Orden | Etapa | IDs | Requiere contrato nuevo | Estado |
|---|---|---|---|---|
| 1 | Cierre de HU39 | A1–A9 | No | Hecho (2026-11-09) |
| 2 | Duplicados concurrentes de cuentas → `409` | U2, U8, U9 | No | Hecho (PR #40) |
| 3 | Reportes de matrícula y asistencia | R1–R4 | No (ya contratados) | Hecho (PR #41) |
| 4 | Indicadores del Proceso 3 | R5 | Sí (aprobado el 2026-11-10) | Implementada (pendiente de revisión) |
| 5 | Ampliación de la lista de asistencia | A7, A8 | Sí (aprobado el 2026-11-10) | Implementada (pendiente de revisión) |
| 6 | Correo y notificaciones | MER `notifications` | Sí | Pendiente (al final) |
| 7 | Recuperación de contraseña | Nuevos, bajo `/auth` | Sí, más tabla en el MER | Pendiente (al final) |
| 8 | Calidad y despliegue | Transversal | No | Pendiente (cierre) |

### Etapa 1 — Cierre de HU39
**Estado (2026-11-09): hecho.** `./gradlew check` completo sobre `development` (commit `9ad02ff`), con PostgreSQL 18.6 en Testcontainers: 369 pruebas, 0 fallos, 0 omitidas; cobertura JaCoCo de líneas 99,6 % e instrucciones 99,5 %. No hubo que corregir código.

### Etapa 2 — Duplicados concurrentes de cuentas (RNF-11, RNF-13)
- Hoy dos registros simultáneos con el mismo correo o documento responden `500`, porque la violación de la restricción única de PostgreSQL no se traduce.
- Traducir esa violación al `409` de duplicado correspondiente (correo o documento) sin exponer detalles internos, en U2 y en la creación y edición de usuarios (U8, U9).
- Pruebas de integración con solicitudes concurrentes.

**Estado (2026-11-10): integrada en `development` (PR #40).**
- `AccountCreator` escribe la cuenta (U2, U8 y el administrador inicial) o el cambio de correo (U9, nuevo método `changeEmail`) y la envía a la base dentro del caso de uso. Si la base la rechaza, `AccountConflictLookup` consulta en una transacción nueva (`REQUIRES_NEW`, solo lectura) qué dato ya está registrado y se responde `409 EMAIL_ALREADY_REGISTERED` o `409 ID_NUMBER_ALREADY_REGISTERED`, conservando la causa original. Si no es el correo ni el documento, el error se relanza sin cambios.
- La consulta en una transacción nueva es necesaria porque PostgreSQL aborta la transacción que recibió el error; así la traducción tampoco depende de los nombres de las restricciones de la base desplegada, que se crea fuera de la aplicación.
- `BusinessException` admite una causa, para no perder el error original.
- Pruebas: `AccountCreatorTest` (7) y `DuplicateAccountRaceIntegrationTest` (3). La prueba de integración reproduce la carrera de forma determinista: una transacción de la prueba inserta la cuenta sin confirmar, la solicitud queda bloqueada en el índice único y, al confirmar la primera, debe recibir `409`. Contra el código anterior las dos pruebas de registro fallan con `500`.
- `./gradlew check`: 379 pruebas en verde; cobertura de líneas 99,6 %.
- Fuera de esta etapa: los mismos duplicados simultáneos en deportistas (`DUPLICATE_ATHLETE`) y disciplinas (`DUPLICATE_DISCIPLINE`) pueden seguir respondiendo `500`.

### Etapa 3 — Reportes R1–R4 (KPI de los Procesos 1 y 2)
- Contrato ya definido en `openapi.yaml`; cubre los KPI de la Definición:
  - R1 (solo `ADMIN`): inscripciones por periodo y estado, tasas de completitud y abandono, tiempo promedio de confirmación, desglose por disciplina y pagos.
  - R2 (`ADMIN`, `COACH`): sesiones programadas frente a realizadas, cobertura de registro (RNF-10), asistencia por grupo y disciplina.
  - R3 (`ADMIN`, `COACH`, `GUARDIAN`): asistencia de un deportista, con verificación de propiedad.
  - R4 (`ADMIN`, `COACH`): ausencias consecutivas con umbral configurable.
- `ReportController` y `ReportService` (SAD 2.4.2), con consultas agregadas que eviten N+1; SQL nativo solo si se justifica (ADR-06).
- Alcance por rol: el entrenador solo sus grupos y el acudiente solo sus deportistas.
- Puede dividirse en dos HU: R1 (Proceso 1) y R2–R4 (Proceso 2).

**Estado (2026-11-10): integrada en `development` (PR #41).** La corrección de SonarCloud separó `DateRangePolicy.requireReportRange` (rango cerrado, R2) de `requireOpenReportRange` (admite extremos nulos, R3).

Reglas decididas el 2026-11-10:
- R1: las matrículas del periodo son las que tienen ese mes de inicio (`period_month`). `completionRate` = `ACTIVE` / total y `abandonmentRate` = `CANCELLED` / total. `averageConfirmationHours` promedia `confirmedAt - createdAt` de las `ACTIVE` del periodo (`null` si no hay). `payments` cuenta el estado de pago actual de las matrículas `ACTIVE` vigentes en el periodo (iniciadas ese mes o antes), porque el estado de pago no guarda historia.
- Fechas: R2 sin fechas abarca del primer día del mes de `dateTo` (hoy por defecto) hasta `dateTo`, y solo cuenta sesiones con fecha hasta hoy. R3 sin fechas abarca todo el historial. Con ambos extremos el rango máximo es de 366 días; un rango invertido o más largo responde `422 INVALID_DATE_RANGE` (`DateRangePolicy.requireReportRange`).
- Alcance: el entrenador solo ve sus grupos actuales (como en A9); un filtro de grupo o disciplina ajeno devuelve datos vacíos, no `403`. En R3 el entrenador solo puede consultar a un deportista con matrícula vigente en uno de sus grupos y solo ve las marcas de esos grupos; el acudiente, solo a sus deportistas. R4 solo incluye deportistas con matrícula vigente en el grupo.
- Porcentajes con un decimal (redondeo hacia arriba en la mitad). Sin base, las tasas obligatorias valen 0 y las opcionales (`attendanceRate`) son `null`.
- R4 cuenta, por deportista y grupo, las marcas `ABSENT` seguidas desde la más reciente; `lastAttendance` es la fecha de la última `PRESENT`. Se calcula en memoria sobre una proyección ordenada, sin SQL nativo (ADR-06). Orden: racha descendente y luego apellidos y nombres en español (sin distinguir tildes ni mayúsculas).

Implementación:
- `ReportController` (`/reports`), `EnrollmentReportService` (R1), `AttendanceReportService` (R2, R3) y `RecurrentAbsenceService` (R4), todos de solo lectura; consultas JPQL agregadas con proyecciones, sin N+1. Se separaron en tres servicios en lugar de un único `ReportService` para mantener una responsabilidad por clase.
- `SecurityConfig`: `/reports/enrollments` solo `ADMIN`; `/reports/attendance/athletes/*` los tres roles; el resto de `/reports/**`, `ADMIN` y `COACH`.
- Las fechas opcionales de las consultas usan `cast(:fecha as LocalDate) is null`, porque PostgreSQL no puede inferir el tipo de un parámetro nulo.
- Pruebas: `EnrollmentReportServiceTest`, `AttendanceReportServiceTest`, `RecurrentAbsenceServiceTest`, `PercentagesTest`, `DateRangePolicyTest` y `ReportIntegrationTest` (cálculos contra PostgreSQL, roles permitidos y denegados, alcance del entrenador, propiedad del acudiente, `404`, `422` y `400`).
- `./gradlew check`: 405 pruebas en verde (404 en la corrida completa más la de desempates de R4, ejecutada aparte); cobertura de líneas 99,7 % y de ramas 96,0 %.

### Etapa 4 — Indicadores del Proceso 3
- KPI de la Definición P3: cuentas registradas por rol y periodo, porcentaje de acudientes con deportistas vinculados y número de cuentas inactivas.
- Diseñar el endpoint en `openapi.yaml` (propuesto: `R5`, `GET /reports/users`, solo `ADMIN`) y aprobarlo antes de implementarlo.

**Estado (2026-11-10): implementada en `feature/user-indicators-and-attendance-roster`, pendiente de revisión y PR.**

Reglas decididas el 2026-11-10 (contrato aprobado):
- `period` (`YYYY-MM`) es obligatorio, como en R1. `registeredInPeriod` cuenta las cuentas creadas ese mes en la zona de la academia, aunque hoy estén inactivas.
- `active`, `inactive`, `inactiveAccounts` y `guardiansWithAthletes` son el estado actual, porque `users.status` no guarda historia (igual que el estado de pago en R1).
- `guardianLinkRate` = acudientes activos con al menos un deportista / acudientes activos (0 si no hay acudientes activos).
- `byRole` siempre trae `ADMIN`, `COACH` y `GUARDIAN`, en ese orden, con ceros si no hay cuentas.

Implementación:
- `UserReportService` (solo lectura) con dos consultas JPQL agregadas en `UserRepository` (`countCreatedByRole`, `countByRoleAndStatus`) y una en `GuardianRepository` (`countActiveWithAthletes`, con `exists`). Endpoint en `ReportController`; `SecurityConfig` agrupa `/reports/enrollments` y `/reports/users` como reportes solo `ADMIN`.
- Pruebas: `UserReportServiceTest` (cálculos, límites del mes en la zona de la academia, ceros) y `ReportIntegrationTest` (diferencias antes y después contra PostgreSQL, periodo sin cuentas, `403` para `COACH` y `GUARDIAN`, `401` y `400`).

### Etapa 5 — Ampliación de A7
- Añadir a la lista de asistencia la edad del deportista y el contacto del acudiente (nombre y teléfono), solo para el entrenador del grupo y el administrador.
- Cambio de la respuesta de A7 en `openapi.yaml`; requiere aprobación.

**Estado (2026-11-10): implementada en `feature/user-indicators-and-attendance-roster`, pendiente de revisión y PR.**
- Contrato aprobado el 2026-11-10: `AttendanceRosterEntry` añade `age` y `guardian` (`GuardianContact`: nombres, apellidos y teléfono nulable). A8 devuelve el mismo esquema. El frontend debe adaptarse.
- La edad se calcula con la fecha actual de la academia (`AthleteAgePolicy.ageOn`), no con la de la sesión, porque es para emergencias.
- `EnrollmentRepository.findRosterAthletes` carga el acudiente y su cuenta con `join fetch` en la misma consulta, sin N+1. `AttendanceMapper` solo expone nombres, apellidos y teléfono de la cuenta del acudiente.
- El acceso no cambia: solo `ADMIN` y el entrenador actual del grupo; otro entrenador y el acudiente reciben `403` (probado en `SessionAttendanceIntegrationTest`).
- Pruebas: `AttendanceServiceTest` (edad a la fecha actual en una sesión de hace un año, contacto del acudiente) y `SessionAttendanceIntegrationTest` (campos nuevos contra PostgreSQL, sin correo del acudiente, accesos denegados).

Cierre de las etapas 4 y 5: `./gradlew check` el 2026-11-10 con PostgreSQL 18.6 en Testcontainers: 413 pruebas, 0 fallos, 0 omitidas; cobertura JaCoCo de líneas 99,7 %, instrucciones 99,6 % y ramas 96,0 %.

### Etapa 6 — Correo y notificaciones (al final)
- Definir el catálogo de eventos: registro de cuenta, recuperación de contraseña, inscripción, confirmación y rechazo, falta de cupos (`NO_SPOTS_AVAILABLE`), horarios publicados (`SCHEDULE_PUBLISHED`) y novedades (`SCHEDULE_CHANGE`).
- Definir el ciclo de vida en `notifications` (persistencia antes o después del envío, estado, intentos).
- `NotificationService` y adaptador Brevo desacoplado (ADR-09), con envíos posteriores al commit, reintentos y registro de fallos sin afirmar envíos no confirmados.
- Pruebas con el proveedor simulado y medición de RNF-14 (≥ 95 %).

### Etapa 7 — Recuperación de contraseña (al final, después del correo)
- Contrato de solicitud y restablecimiento (públicos, bajo `/auth`) y tabla de tokens en el MER (usuario, hash del token, vencimiento, fecha de uso y creación).
- Token aleatorio de un solo uso, guardado solo como hash, con vencimiento corto; la solicitud responde igual exista o no el correo; el restablecimiento aplica `PasswordPolicy`.

### Etapa 8 — Calidad y despliegue (cierre)
- Ejecutar la suite completa y revisar la cobertura (JaCoCo) y el Javadoc (RNF-17, ≥ 80 %).
- Medir con evidencia reproducible los RNF de rendimiento (RNF-04, RNF-06, RNF-07) y de seguridad (RNF-13, RNF-15, RNF-16).
- Validar CORS, secretos y configuración general.
- Mantener el pipeline de GitHub Actions y el despliegue en Railway (ADR-10).

### Pendiente de confirmar
- Estado de pago consolidado por deportista: E7 ya filtra por `athleteId` y devuelve el `paymentStatus` de cada matrícula. Se propone darlo por cubierto sin endpoint nuevo.

## Mejoras futuras (fuera de la Entrega 3)
Decididas el 2026-11-09; se retoman solo si queda tiempo al final del proyecto:
- Rechazo de tokens vigentes de cuentas inactivadas (RNF-13). Hoy el login ya rechaza las cuentas inactivas (`USER_INACTIVE`), pero un token emitido antes de la inactivación sigue vigente hasta vencer.
- Inactivación de deportistas para atender la supresión de datos (estado en `athletes` en el MER y cambio de `openapi.yaml`).
- Versión de la política de datos aceptada.
- Auditoría de consultas a datos de menores.
