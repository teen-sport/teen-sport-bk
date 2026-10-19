# 09. Matriz de trazabilidad

Esta matriz vincula los procesos de negocio de la Definición del Proyecto (v02), las familias de endpoints del OpenAPI, los componentes y las pruebas. Los RNF corresponden al Plan de Gestión de la Calidad v02 (ver `03-requisitos-no-funcionales.md`).

## Matriz por proceso

| Proceso | IDs API | Dominio | Componentes principales | Pruebas mínimas | RNF |
|---|---|---|---|---|---|
| P3 | U1–U5 | Autenticación y perfil | AuthController/AuthService, UserController, MyAccountService, GuardianRegistrationService, SecurityConfig, JWT, DTO | Login válido/inválido, cuenta inactiva, expiración JWT, cambio de contraseña, campos protegidos, duplicados | RNF-07, RNF-11, RNF-13, RNF-15, RNF-16 |
| P3 | U6–U12 | Administración de usuarios | UserAdminController, UserDirectoryController, UserAdministrationService, UserDirectoryService, UserRepository | Roles, duplicados, activación e inactivación, paginación y búsqueda | RNF-11, RNF-13, RNF-15 |
| P3 | E1–E4 | Perfiles de deportistas | AthleteController/AthleteService, AthleteAccessPolicy, AthleteRepository | Edad, vinculación al acudiente, duplicados, acceso a recursos ajenos | RNF-02, RNF-12, RNF-15 |
| P3 | Pendiente | Recuperación de contraseña | AuthController/AuthService, NotificationService | Token vencido, reutilizado o alterado; respuesta sin revelar cuentas | RNF-13, RNF-14 |
| P3 | Pendiente | Indicadores de usuarios | ReportController/ReportService | Conteos por rol y periodo, acudientes con deportistas, cuentas inactivas | RNF-15 |
| P1 | H1–H5 | Disciplinas | DisciplineController/Service/Repository | Mensualidad positiva, duplicados, desactivación con grupos activos | RNF-13, RNF-15 |
| P1 | H6–H14 | Grupos y horarios | GroupController/Service, ScheduleController/Service, ScheduleRepository | Capacidad 1–15, entrenador activo, horarios y permisos | RNF-01, RNF-15 |
| P1 | E5–E15 | Disponibilidad y matrículas | EnrollmentService, EnrollmentLifecycleService, SpotCounter, GroupRepository, EnrollmentRepository | Cupos concurrentes, transiciones de estado, reasignación de grupo, liberación de cupo | RNF-01, RNF-02, RNF-03, RNF-04, RNF-15 |
| P1 | E9–E11 (reservados) | Documentación de la matrícula | Pendiente | Confirmación bloqueada sin documentos validados, acceso solo del dueño y del administrador | RNF-01, RNF-03, RNF-15 |
| P1 | E16–E19 | Pagos presenciales | PaymentService, PaymentRecorder, PaymentQueryService, PaymentHistoryRepository | Reporte no cambia estado, importe mínimo, duplicado, roles y pertenencia | RNF-01, RNF-03, RNF-15 |
| P1 | R1 | Indicadores de matrícula | ReportController/ReportService | Conteos por estado, tasas de completitud y abandono, tiempo promedio de confirmación | RNF-15 |
| P2 | A1–A6 | Sesiones | SessionController, SessionService, SessionQueryService, SessionSlotPolicy, TrainingSessionRepository | Fechas, cancelación, permisos de entrenador, transiciones de estado | RNF-06, RNF-15 |
| P2 | A7–A9 | Asistencia | AttendanceController, AttendanceService, AttendanceQueryService, AttendanceRecordRepository | Lista elegible, upsert, sesión futura/cancelada, filtros por propiedad | RNF-05, RNF-06, RNF-10, RNF-15 |
| P2 | R2–R4 | Indicadores de asistencia | ReportController/ReportService | Agregaciones, rangos de fecha, aislamiento de datos por rol | RNF-06, RNF-15 |
| Transversal | MER `notifications` | Notificaciones | NotificationService, adaptador Brevo (fuera de la entrega actual) | Fallos de envío, reintentos, registro de estado y privacidad; confirmar contrato | RNF-14 |
| Transversal | — | Documentación de código | Todo el backend | Medición de cobertura Javadoc | RNF-17 |

Los torneos quedaron fuera del alcance (Definición v02) y no tienen fila en la matriz.

## Historias de usuario de la Fase 1

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU28 Registro e inicio de sesión | U1, U2 | Integrada en `development` (PR #11) | `AuthServiceTest`, `GuardianRegistrationServiceTest`, `PasswordPolicyTest`, `JwtTokenServiceTest`, `SecurityErrorHandlerTest`, `SecurityPropertiesTest`, `LoginIntegrationTest`, `GuardianRegistrationIntegrationTest`, `SecurityIntegrationTest`, `UserRepositoryIntegrationTest`, `ReferenceDataInitializerIntegrationTest` | RNF-07, RNF-11, RNF-13, RNF-15, RNF-16 |
| HU29 Mi perfil y contraseña | U3, U4, U5 | Integrada en `development` (PR #12) | `MyAccountServiceTest`, `CurrentUserServiceTest`, `MyAccountIntegrationTest`, `SecurityIntegrationTest` | RNF-13, RNF-15 |
| HU30 Administración de usuarios | U6–U12 | Integrada en `development` (PR #13); `COACH_HAS_ACTIVE_GROUPS` pendiente de la Fase 3 | `UserAdministrationServiceTest`, `UserDirectoryServiceTest`, `UserAdministrationIntegrationTest`, `GlobalExceptionHandlerTest` | RNF-11, RNF-13, RNF-15 |

## Historias de usuario de la Fase 2

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU32 Gestión de disciplinas | H1–H5 | Integrada en `development` (PR #17); `DISCIPLINE_HAS_ACTIVE_GROUPS` pendiente de la Fase 3 | `DisciplineServiceTest`, `DisciplineIntegrationTest` | RNF-13, RNF-15 |
| HU34 Registro y gestión de deportistas | E1–E4 | Integrada en `development` (PR #18); `hasActiveEnrollment` real desde HU35 | `AthleteServiceTest`, `AthleteAgePolicyTest`, `AcademyCalendarTest`, `AthleteIntegrationTest` | RNF-02, RNF-12, RNF-13, RNF-15 |

## Historias de usuario de la Fase 3

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU33 Gestión de grupos, horario y entrenador | H6–H14; cierra U10 `COACH_HAS_ACTIVE_GROUPS` y H5 `DISCIPLINE_HAS_ACTIVE_GROUPS` | Integrada en `development` (PR #19); `CAPACITY_BELOW_OCCUPIED` y `GROUP_HAS_ACTIVITY` por matrículas se cierran en HU35; las sesiones de H10 y H11 se cierran en HU39 | `TimeSlotTest`, `GroupServiceTest`, `ScheduleServiceTest`, `GroupIntegrationTest`, `ScheduleIntegrationTest` (incluye concurrencia), `UserAdministrationServiceTest`, `DisciplineServiceTest` | RNF-01, RNF-13, RNF-15 |

## Historias de usuario de la Fase 4

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU35 Solicitud de matrícula | E5–E8; cierra H9 `CAPACITY_BELOW_OCCUPIED`, H10 `GROUP_HAS_ACTIVITY` (matrículas) y `hasActiveEnrollment` (E1–E4) | Integrada en `development` (PR #20); correo `NO_SPOTS_AVAILABLE` pendiente de la integración de correo | `EnrollmentServiceTest`, `EnrollmentQueryServiceTest`, `AvailabilityServiceTest`, `SpotCounterTest`, `PeriodPolicyTest`, `GroupServiceTest`, `AthleteServiceTest`, `EnrollmentIntegrationTest` (incluye concurrencia), `AvailabilityIntegrationTest` | RNF-01, RNF-02, RNF-03, RNF-13, RNF-15 |
| HU37 Revisión y ciclo de la matrícula | E12–E15 | Integrada en `development` (PR #21); `payment` en E12 cerrado en HU36; correos de E12/E13 pendientes de la integración de correo; validación de documentos pendiente | `EnrollmentLifecycleServiceTest`, `EnrollmentLifecycleIntegrationTest` (incluye concurrencia) | RNF-01, RNF-03, RNF-04, RNF-13, RNF-15 |

## Historias de usuario de la Fase 5

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU36 Gestión de pagos | E16–E19; cierra `payment` en E12 | Integrada en `development` (PR #22) | `PaymentServiceTest`, `PaymentQueryServiceTest`, `EnrollmentLifecycleServiceTest`, `PaymentIntegrationTest` (incluye concurrencia) | RNF-01, RNF-03, RNF-13, RNF-15 |

## Historias de usuario de la Fase 6

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU39 Registro y consulta de asistencia | A1–A9; cierra las sesiones futuras de H10 y H11 | Implementada (pendiente de revisión); correos `SCHEDULE_PUBLISHED` y `SCHEDULE_CHANGE` pendientes de la integración de correo | `SessionServiceTest`, `SessionQueryServiceTest`, `AttendanceServiceTest`, `AttendanceQueryServiceTest`, `DateRangePolicyTest`, `GroupServiceTest`, `SessionAttendanceIntegrationTest` (incluye concurrencia) | RNF-05, RNF-10, RNF-13, RNF-15 |

La columna RNF se reasignó el 2026-10-19 según el Plan de Calidad v02: hasta entonces RNF-11 significaba "integridad de datos entre procesos" y se usaba en HU33, HU35, HU36, HU37 y HU39; ahora es "unicidad de cuentas de usuario". La asignación indica qué RNF verifica cada HU, no que su métrica esté cumplida.

## Requisitos no funcionales
Relacionar cada prueba con RNF-01 a RNF-17 cuando corresponda, con las métricas exactas del Plan de Calidad v02.

## Criterio de cierre
No marcar una fila como cumplida hasta que el código, la prueba automatizada y la evidencia de ejecución existan.
