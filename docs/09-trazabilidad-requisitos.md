# 09. Matriz de trazabilidad

Esta matriz inicial vincula las familias de endpoints del OpenAPI con capas y pruebas. Debe ampliarse con los IDs oficiales de requisitos del proyecto cuando exista una matriz de requisitos aprobada.

| IDs API | Dominio | Componentes principales | Pruebas mínimas |
|---|---|---|---|
| U1–U5 | Autenticación y perfil | AuthController/AuthService, UserController/UserService, SecurityConfig, JWT, DTO | Login válido/inválido, expiración JWT, cambio de contraseña, campos protegidos |
| U6–U12 | Administración de usuarios | UserController/UserService, UserRepository, Guardian/Coach profiles | Roles, duplicados, estado inactivo, paginación y búsqueda |
| E1–E4 | Deportistas | AthleteController/AthleteService, AthleteRepository | Edad, pertenencia, duplicados, acceso a recursos ajenos |
| E5–E15 | Disponibilidad y matrículas | EnrollmentService, GroupRepository, EnrollmentRepository, transacciones | Cupos concurrentes, transiciones de estado, reasignación de grupo, liberación de cupo |
| E16–E19 | Pagos presenciales | PaymentService, PaymentHistoryRepository | Reporte no cambia estado, importe mínimo, duplicado, roles y pertenencia |
| H1–H5 | Disciplinas | DisciplineController/Service/Repository | Mensualidad positiva, duplicados, desactivación con grupos activos |
| H6–H14 | Grupos y horarios | GroupController/Service, ScheduleRepository | Capacidad 1–15, entrenador activo, horarios y permisos |
| A1–A6 | Sesiones | SessionController/Service/Repository | Fechas, cancelación, permisos de entrenador, transiciones de estado |
| A7–A9 | Asistencia | AttendanceController/Service/Repository | Lista elegible, upsert, sesión futura/cancelada, filtros por propiedad |
| R1–R4 | Informes | ReportController/ReportService, repositorios/consultas | Agregaciones, rangos de fecha, aislamiento de datos por rol |
| MER torneos | Torneos | Pendiente de contrato | Definir requisitos, endpoints, permisos y pruebas antes de implementar |
| MER notificaciones | Notificaciones | NotificationService/Brevo adapter | Fallos de envío, reintentos, registro de estado y privacidad; confirmar contrato |

## Historias de usuario de la Fase 1

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU28 Registro e inicio de sesión | U1, U2 | Integrada en `development` (PR #11) | `AuthServiceTest`, `GuardianRegistrationServiceTest`, `PasswordPolicyTest`, `JwtTokenServiceTest`, `SecurityErrorHandlerTest`, `SecurityPropertiesTest`, `LoginIntegrationTest`, `GuardianRegistrationIntegrationTest`, `SecurityIntegrationTest`, `UserRepositoryIntegrationTest`, `ReferenceDataInitializerIntegrationTest` | RNF-13, RNF-15, RNF-16 |
| HU29 Mi perfil y contraseña | U3, U4, U5 | Integrada en `development` (PR #12) | `MyAccountServiceTest`, `CurrentUserServiceTest`, `MyAccountIntegrationTest`, `SecurityIntegrationTest` | RNF-13, RNF-15 |
| HU30 Administración de usuarios | U6–U12 | Integrada en `development` (PR #13); `COACH_HAS_ACTIVE_GROUPS` pendiente de la Fase 3 | `UserAdministrationServiceTest`, `UserDirectoryServiceTest`, `UserAdministrationIntegrationTest`, `GlobalExceptionHandlerTest` | RNF-13, RNF-15 |

## Historias de usuario de la Fase 2

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU32 Gestión de disciplinas | H1–H5 | Integrada en `development` (PR #17); `DISCIPLINE_HAS_ACTIVE_GROUPS` pendiente de la Fase 3 | `DisciplineServiceTest`, `DisciplineIntegrationTest` | RNF-13, RNF-15 |
| HU34 Registro y gestión de deportistas | E1–E4 | Integrada en `development` (PR #18); `hasActiveEnrollment` real desde HU35 | `AthleteServiceTest`, `AthleteAgePolicyTest`, `AcademyCalendarTest`, `AthleteIntegrationTest` | RNF-13, RNF-15 |

## Historias de usuario de la Fase 3

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU33 Gestión de grupos, horario y entrenador | H6–H14; cierra U10 `COACH_HAS_ACTIVE_GROUPS` y H5 `DISCIPLINE_HAS_ACTIVE_GROUPS` | Integrada en `development` (PR #19); `CAPACITY_BELOW_OCCUPIED` y `GROUP_HAS_ACTIVITY` por matrículas se cierran en HU35; las sesiones (H10, H11) dependen de la Fase 6 | `TimeSlotTest`, `GroupServiceTest`, `ScheduleServiceTest`, `GroupIntegrationTest`, `ScheduleIntegrationTest` (incluye concurrencia), `UserAdministrationServiceTest`, `DisciplineServiceTest` | RNF-11, RNF-13, RNF-15 |

## Historias de usuario de la Fase 4

| HU | IDs API | Estado | Pruebas automatizadas | RNF |
|---|---|---|---|---|
| HU35 Solicitud de matrícula | E5–E8; cierra H9 `CAPACITY_BELOW_OCCUPIED`, H10 `GROUP_HAS_ACTIVITY` (matrículas) y `hasActiveEnrollment` (E1–E4) | Integrada en `development` (PR #20); correo `NO_SPOTS_AVAILABLE` pendiente de la Fase 7 | `EnrollmentServiceTest`, `EnrollmentQueryServiceTest`, `AvailabilityServiceTest`, `SpotCounterTest`, `PeriodPolicyTest`, `GroupServiceTest`, `AthleteServiceTest`, `EnrollmentIntegrationTest` (incluye concurrencia), `AvailabilityIntegrationTest` | RNF-11, RNF-13, RNF-15 |
| HU37 Revisión y ciclo de la matrícula | E12–E15 | Implementada (pendiente de revisión); `payment` en E12 pendiente de la Fase 5 y correos de E12/E13 de la Fase 7 | `EnrollmentLifecycleServiceTest`, `EnrollmentLifecycleIntegrationTest` (incluye concurrencia) | RNF-11, RNF-13, RNF-15 |

## Requisitos no funcionales
Relacionar cada prueba con RNF-04 a RNF-17 cuando corresponda. Los RNF-01 a RNF-03 deben transcribirse desde el plan de calidad original para preservar sus métricas exactas.

## Criterio de cierre
No marcar una fila como cumplida hasta que el código, la prueba automatizada y la evidencia de ejecución existan.
