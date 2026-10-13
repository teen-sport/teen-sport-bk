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
| HU30 Administración de usuarios | U6–U12 | Implementada (pendiente de revisión); `COACH_HAS_ACTIVE_GROUPS` pendiente de la Fase 3 | `UserAdministrationServiceTest`, `UserDirectoryServiceTest`, `UserAdministrationIntegrationTest`, `GlobalExceptionHandlerTest` | RNF-13, RNF-15 |

## Requisitos no funcionales
Relacionar cada prueba con RNF-04 a RNF-17 cuando corresponda. Los RNF-01 a RNF-03 deben transcribirse desde el plan de calidad original para preservar sus métricas exactas.

## Criterio de cierre
No marcar una fila como cumplida hasta que el código, la prueba automatizada y la evidencia de ejecución existan.
