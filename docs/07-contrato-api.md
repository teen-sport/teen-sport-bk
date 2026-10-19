# 07. Contrato de la API REST

## Fuente y alcance

Este documento se deriva de `openapi.yaml`. El archivo OpenAPI es la fuente normativa para rutas, métodos, esquemas, permisos declarados y respuestas. No duplicar manualmente sus esquemas sin actualizar el archivo fuente.

La especificación OpenAPI 3.0.3 declara 41 rutas y 55 operaciones HTTP. La versión declarada es 1.0.0.

Servidor local declarado: `http://localhost:8080/api/v1`.

## Convenciones globales

- Autenticación por `Authorization: Bearer <JWT>`, excepto `POST /auth/login` y `POST /users/register`.
- Roles: `ADMIN`, `COACH`, `GUARDIAN`.
- Propiedad: los acudientes acceden a sus deportistas; entrenadores a sus grupos asignados. Las listas deben aplicar filtrado por pertenencia en backend.
- Fechas: `YYYY-MM-DD`; horas `HH:mm`; periodos `YYYY-MM`; timestamps ISO-8601 UTC; valores monetarios numéricos en COP con hasta dos decimales (`DECIMAL(10,2)` en el MER); identificadores enteros de 32 bits (`int32`, `INT` en el MER).
- `200`: lectura/actualización; `201`: creación y cabecera `Location`; `204`: sin cuerpo; `400`: solicitud/campos inválidos; `401`: no autenticado; `403`: no autorizado; `404`: no encontrado; `409`: duplicado/conflicto de estado; `422`: regla de negocio incumplida.
- Matrícula y periodo: el `period` de una matrícula es su mes de inicio; la matrícula sigue vigente mes a mes hasta ser rechazada o cancelada. Los cupos ocupados de un mes son las matrículas `PENDING_REVIEW` y `ACTIVE` del grupo con mes de inicio igual o anterior (E5, E6, H9, A7).
- Matrícula duplicada: un deportista no puede tener dos matrículas vigentes (`PENDING_REVIEW` o `ACTIVE`) en la misma disciplina, sin importar el grupo ni el mes de inicio; E6 devuelve `409 DUPLICATE_ENROLLMENT`. Sí puede tener matrículas vigentes en disciplinas distintas. Una matrícula rechazada o cancelada no bloquea una nueva. El cambio de grupo se hace con E15.
- Pagos: una matrícula puede tener varios pagos, como máximo uno por mes calendario. El mes que cubre un pago es el mes de su `paymentDate` (campo `period` de `Payment`) y el pago con `paymentDate` más reciente es el último. Un segundo pago en un mes que ya tiene pago devuelve `409 PAYMENT_ALREADY_REGISTERED` (E12, E16, E17).
- Pagos presenciales (E16–E19, HU36): solo el acudiente dueño reporta (E16) y solo `ADMIN` registra (E17) o marca la mora (E18). E16 y E17 exigen la matrícula vigente (`409 INVALID_STATE`); E18 se admite en cualquier estado. El reporte solo activa `paymentReported`, guarda el momento del reporte y su nota, y usa la fecha indicada (o el mes actual) solo para detectar un mes ya pagado. El registro exige un monto de al menos la mensualidad de la disciplina (`422 INSUFFICIENT_AMOUNT`) y una fecha no posterior a hoy en la zona de la academia (`422 FUTURE_DATE`); deja la matrícula `UP_TO_DATE` y `paymentReported=false`. `receiptReference` admite 50 caracteres (el MER admite 100). E19 ordena por `paymentDate` descendente y rechaza `from` posterior a `to` con `400`.
- Perfil propio (U4): reemplazo completo. Se aplican solo los campos del rol del usuario (`homeAddress` para acudientes; `specialty` y `yearsOfExperience` para entrenadores); los de otro rol, el correo, el documento y el rol enviados en el cuerpo se ignoran. Un campo opcional del rol que no se envía queda vacío.
- Administración (U6–U12): solo `ADMIN`; un `COACH` o `GUARDIAN` recibe `403 ACCESS_DENIED` antes de que se valide el cuerpo. Los listados se ordenan por apellidos y nombres. `q` busca sin distinguir mayúsculas en nombres, apellidos, nombre completo, correo y documento (`%` y `_` se buscan como texto). U8 solo acepta `role` `ADMIN` o `COACH` (otro valor: `400` en el campo `staffRole`). U9 conserva el correo si no se envía y nunca cambia rol, documento ni estado. U10 rechaza que un administrador desactive su propia cuenta (`422 CANNOT_DEACTIVATE_SELF`); `COACH_HAS_ACTIVE_GROUPS` se aplica desde HU33.
- Disciplinas (H1–H5): cualquier rol autenticado consulta; solo `ADMIN` crea, edita o cambia el estado (los demás reciben `403` antes de validar el cuerpo). Quien no es administrador solo ve disciplinas `ACTIVE` en H1 y el filtro `status` se ignora. El nombre es único sin distinguir mayúsculas (`409 DUPLICATE_DISCIPLINE`). `monthlyFee` > 0 con hasta 8 enteros y 2 decimales. `DISCIPLINE_HAS_ACTIVE_GROUPS` se aplica desde HU33.
- Deportistas (E1–E4): solo `ADMIN` y `GUARDIAN` (un `COACH` recibe `403`). El acudiente accede solo a sus deportistas (`403 ACCESS_DENIED` sobre los ajenos; en E2 el filtro `guardianId` se ignora para él) y registra siempre a su nombre. El administrador debe indicar un `guardianId` de un acudiente activo (`400` en `guardianId`). La edad (12–17) se calcula con la fecha local de la academia (`APP_TIME_ZONE`, por defecto `America/Bogota`); en E4 solo se valida si cambia `birthDate`. El documento es único entre deportistas (`409 DUPLICATE_ATHLETE`). `hasActiveEnrollment` indica si el deportista tiene una matrícula `PENDING_REVIEW` o `ACTIVE` (desde HU35).
- Grupos y franjas (H6–H14): `GET /groups/**` para `ADMIN` y `COACH` (el entrenador solo ve sus grupos; para él `coachId` se ignora y un grupo ajeno responde `403`); el resto solo `ADMIN`. Capacidad de 1 a 15 (`422 INVALID_MAX_CAPACITY`). Disciplina o entrenador inexistente → `404`; inactivo → `422 DISCIPLINE_INACTIVE` / `INVALID_COACH`. Dos franjas se cruzan si son del mismo día y sus rangos `[inicio, fin)` se solapan (las contiguas no se cruzan); solo cuentan los grupos `ACTIVE` del entrenador. Se pueden gestionar franjas de grupos inactivos; por eso, al reactivar un grupo (H10) se rechaza con `409 COACH_SCHEDULE_CONFLICT` si sus franjas se cruzan con las de otro grupo activo del entrenador. Desde HU35, H9 rechaza una capacidad menor que las matrículas vigentes del grupo (`409 CAPACITY_BELOW_OCCUPIED`) y H10 no desactiva un grupo con matrículas vigentes (`409 GROUP_HAS_ACTIVITY`); desde HU39, H10 tampoco desactiva un grupo con sesiones futuras y H11 traspasa las sesiones futuras al nuevo entrenador. Desde HU33 se aplican `COACH_HAS_ACTIVE_GROUPS` (U10) y `DISCIPLINE_HAS_ACTIVE_GROUPS` (H5).
- Disponibilidad y matrículas (E5–E8): solo `ADMIN` y `GUARDIAN` (un `COACH` recibe `403`). El acudiente solo matricula y ve las matrículas de sus deportistas (`403 ACCESS_DENIED`; en E7 el filtro `guardianId` se ignora para él). Como una matrícula vigente ocupa cupo en todos los meses desde su inicio, E6 (y H9) comparan la capacidad con todas las matrículas vigentes del grupo; en E5, `occupiedSpots` cuenta las que empezaron en el periodo o antes y `availableSpots` es la capacidad menos todas las vigentes (mínimo 0). Orden de validación de E6: deportista y grupo existentes (`404`), propiedad (`403`), edad (`422 AGE_OUT_OF_RANGE`), periodo (`422 PERIOD_IN_PAST`), grupo y disciplina activos (`422 GROUP_INACTIVE`), duplicado (`409 DUPLICATE_ENROLLMENT`) y cupo (`409 NO_AVAILABLE_SPOTS`). El filtro `period` de E7 devuelve las matrículas vigentes en ese mes (inicio igual o anterior y, sin filtro `status`, en `PENDING_REVIEW` o `ACTIVE`); el listado se ordena de la más reciente a la más antigua. El correo `NO_SPOTS_AVAILABLE` se enviará cuando exista el adaptador de correo (Fase 8).
- Revisión y ciclo (E12–E15): confirmar, rechazar y cambiar de grupo solo `ADMIN` (`403` antes de validar el cuerpo); cancelar `ADMIN` y el acudiente dueño. E12 y E13 solo desde `PENDING_REVIEW`; E14 y E15 solo con la matrícula vigente; si no, `409 INVALID_STATE`. Un acudiente que cancela una matrícula `ACTIVE` recibe `403 ACCESS_DENIED`. El motivo de E13 y, si se envía, el de E14 se guardan en `reviewNotes`. Cambiar de grupo (E12 con `groupId`, E15) exige un grupo de la misma disciplina (`422 GROUP_OTHER_DISCIPLINE`), activo (`422 GROUP_INACTIVE`) y con cupo (`409 NO_AVAILABLE_SPOTS`), y conserva el mes de inicio; indicar el grupo actual no cambia nada. E12 con `payment` registra el pago con las reglas de E17 en la misma transacción; si el pago no las cumple, no se confirma nada. Los correos de E12 y E13 llegan con la Fase 8.
- Sesiones y asistencia (A1–A9, HU39): las sesiones las consultan los tres roles (A3, A4); las programan, reprograman y cancelan `ADMIN` y el entrenador del grupo (A1, A2, A5, A6); la lista de asistencia la ven `ADMIN` y el entrenador del grupo (A7) y solo ese entrenador registra (A8). El entrenador "del grupo" es el actual. Una fecha "pasada" es anterior a hoy en la zona de la academia (hoy se admite) y una sesión "futura" en A8 es la de fecha posterior a hoy. Un grupo no puede tener dos sesiones con la misma fecha y hora de inicio (`409 DUPLICATE_SESSION`); A2 omite las que ya existen. Los rangos de A2 y A3 abarcan como máximo 62 días con ambos extremos; sin fechas, A3 usa la semana actual (lunes a domingo) y con un solo extremo, siete días. Solo las sesiones `SCHEDULED` se reprograman o cancelan (`409 INVALID_STATE`). La lista de A7/A8 son las matrículas `ACTIVE` del grupo iniciadas en el mes de la sesión o antes; A8 exige marcar a todos (`422 INCOMPLETE_ATTENDANCE`), solo a ellos (`422 ATHLETE_NOT_IN_ROSTER`) y una sola vez (`400`); guardar de nuevo reemplaza las marcas y deja la sesión `COMPLETED`. Las marcas de quien sale después de la lista se conservan en A9. El acudiente ve en A3/A4 las sesiones de los grupos de sus deportistas con matrícula `ACTIVE` ya iniciada, con `athletes`. A9 ordena por fecha de sesión descendente. H10 rechaza desactivar un grupo con sesiones `SCHEDULED` de hoy en adelante, y H11 pasa esas sesiones al nuevo entrenador. Los correos `SCHEDULE_PUBLISHED` y `SCHEDULE_CHANGE` llegan con la Fase 8.
- Identidad: las operaciones `/users/me` toman al usuario del JWT. Si la cuenta del token ya no existe, responden `401 UNAUTHENTICATED`.
- Contraseñas: de 8 a 72 caracteres (72 bytes en UTF-8, límite de BCrypt) con al menos una mayúscula, una minúscula, un número y un símbolo. La longitud fuera de rango devuelve `400 VALIDATION_ERROR`; la falta de un tipo de carácter, `422 WEAK_PASSWORD` (U2, U5, U8).
- Errores: el consumidor debe evaluar `code`; `message` es un mensaje de presentación en español.
- `401` usa `UNAUTHENTICATED` (token ausente o inválido), `TOKEN_EXPIRED` (token vencido) o `INVALID_CREDENTIALS` (login fallido).
- `500` usa `INTERNAL_ERROR` con un mensaje genérico en español; nunca expone trazas, SQL ni detalles internos. Aplica a cualquier operación (respuesta `InternalError` en `openapi.yaml`).

## Inventario de operaciones

| ID | Método | Ruta | operationId | Resumen | Roles | Códigos de error declarados |
|---|---|---|---|---|---|---|
| A1 | `POST` | `/sessions` | `createSession` | Create a single training session | ADMIN, COACH | VALIDATION_ERROR, NOT_FOUND, GROUP_INACTIVE, DATE_IN_PAST, INVALID_TIME_RANGE, DUPLICATE_SESSION |
| A2 | `POST` | `/sessions/generate` | `generateSessions` | Generate sessions from the group's weekly schedules | ADMIN, COACH | VALIDATION_ERROR, NOT_FOUND, GROUP_INACTIVE, GROUP_WITHOUT_SCHEDULES, INVALID_DATE_RANGE, DATE_IN_PAST |
| A3 | `GET` | `/sessions` | `listSessions` | Session calendar | ADMIN, COACH, GUARDIAN | INVALID_DATE_RANGE |
| A4 | `GET` | `/sessions/{id}` | `getSession` | Get a session | ADMIN, COACH, GUARDIAN | NOT_FOUND |
| A5 | `PUT` | `/sessions/{id}` | `updateSession` | Reschedule a session | ADMIN, COACH | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE, DATE_IN_PAST, INVALID_TIME_RANGE, DUPLICATE_SESSION |
| A6 | `POST` | `/sessions/{id}/cancel` | `cancelSession` | Cancel a session | ADMIN, COACH | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE |
| A7 | `GET` | `/sessions/{id}/attendance` | `getSessionAttendance` | Session roster with saved marks | COACH, ADMIN | NOT_FOUND |
| A8 | `PUT` | `/sessions/{id}/attendance` | `recordSessionAttendance` | Record attendance for the whole roster | COACH | VALIDATION_ERROR, NOT_FOUND, INCOMPLETE_ATTENDANCE, ATHLETE_NOT_IN_ROSTER, FUTURE_SESSION, SESSION_CANCELLED |
| A9 | `GET` | `/attendance-records` | `listAttendanceRecords` | Attendance history (paginated) | ADMIN, COACH, GUARDIAN | INVALID_DATE_RANGE |
| E1 | `POST` | `/athletes` | `createAthlete` | Register an athlete | GUARDIAN, ADMIN | VALIDATION_ERROR, AGE_OUT_OF_RANGE, DATA_POLICY_NOT_ACCEPTED, DUPLICATE_ATHLETE |
| E12 | `POST` | `/enrollments/{id}/confirm` | `confirmEnrollment` | Confirm an enrollment | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE, GROUP_INACTIVE, GROUP_OTHER_DISCIPLINE, NO_AVAILABLE_SPOTS, PAYMENT_ALREADY_REGISTERED, INSUFFICIENT_AMOUNT, FUTURE_DATE |
| E13 | `POST` | `/enrollments/{id}/reject` | `rejectEnrollment` | Reject an enrollment | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE |
| E14 | `POST` | `/enrollments/{id}/cancel` | `cancelEnrollment` | Cancel an enrollment | GUARDIAN, ADMIN | ACCESS_DENIED, NOT_FOUND, INVALID_STATE |
| E15 | `PATCH` | `/enrollments/{id}/group` | `changeEnrollmentGroup` | Move an enrollment to another group of the same discipline | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE, GROUP_INACTIVE, GROUP_OTHER_DISCIPLINE, NO_AVAILABLE_SPOTS |
| E16 | `POST` | `/enrollments/{id}/payment-report` | `reportPayment` | Guardian reports that they paid in person | GUARDIAN | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE, PAYMENT_ALREADY_REGISTERED |
| E17 | `POST` | `/enrollments/{id}/payments` | `registerPayment` | Register an in-person payment | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE, PAYMENT_ALREADY_REGISTERED, INSUFFICIENT_AMOUNT, FUTURE_DATE |
| E18 | `PATCH` | `/enrollments/{id}/payment-status` | `updatePaymentStatus` | Manually mark an enrollment as overdue | ADMIN | VALIDATION_ERROR, NOT_FOUND, USE_PAYMENT_REGISTRATION |
| E19 | `GET` | `/payments` | `listPayments` | Payment history (paginated) | ADMIN, GUARDIAN | No se declaran códigos específicos |
| E2 | `GET` | `/athletes` | `listAthletes` | List athletes (paginated) | ADMIN, GUARDIAN | No se declaran códigos específicos |
| E3 | `GET` | `/athletes/{id}` | `getAthlete` | Get an athlete | ADMIN, GUARDIAN | NOT_FOUND |
| E4 | `PUT` | `/athletes/{id}` | `updateAthlete` | Update an athlete | ADMIN, GUARDIAN | VALIDATION_ERROR, NOT_FOUND, AGE_OUT_OF_RANGE, DUPLICATE_ATHLETE |
| E5 | `GET` | `/availability` | `getAvailability` | Groups with free spots for a period | ADMIN, GUARDIAN | VALIDATION_ERROR, PERIOD_IN_PAST |
| E6 | `POST` | `/enrollments` | `createEnrollment` | Request an enrollment | GUARDIAN, ADMIN | VALIDATION_ERROR, NOT_FOUND, AGE_OUT_OF_RANGE, PERIOD_IN_PAST, GROUP_INACTIVE, NO_AVAILABLE_SPOTS, DUPLICATE_ENROLLMENT |
| E7 | `GET` | `/enrollments` | `listEnrollments` | List enrollments (paginated) | ADMIN, GUARDIAN | No se declaran códigos específicos |
| E8 | `GET` | `/enrollments/{id}` | `getEnrollment` | Get an enrollment with schedules | ADMIN, GUARDIAN | NOT_FOUND |
| H1 | `GET` | `/disciplines` | `listDisciplines` | List disciplines | ADMIN, COACH, GUARDIAN | No se declaran códigos específicos |
| H10 | `PATCH` | `/groups/{id}/status` | `updateGroupStatus` | Activate or deactivate a group | ADMIN | NOT_FOUND, GROUP_HAS_ACTIVITY, COACH_SCHEDULE_CONFLICT |
| H11 | `PUT` | `/groups/{id}/coach` | `assignGroupCoach` | Assign or change the group coach | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_COACH, COACH_SCHEDULE_CONFLICT |
| H12 | `POST` | `/groups/{id}/schedules` | `createGroupSchedule` | Add a weekly schedule slot to a group | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_TIME_RANGE, SCHEDULE_CONFLICT |
| H13 | `PUT` | `/schedules/{id}` | `updateSchedule` | Update a weekly schedule slot | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_TIME_RANGE, SCHEDULE_CONFLICT |
| H14 | `DELETE` | `/schedules/{id}` | `deleteSchedule` | Delete a weekly schedule slot | ADMIN | NOT_FOUND |
| H2 | `GET` | `/disciplines/{id}` | `getDiscipline` | Get a discipline | ADMIN, COACH, GUARDIAN | NOT_FOUND |
| H3 | `POST` | `/disciplines` | `createDiscipline` | Create a discipline | ADMIN | VALIDATION_ERROR, DUPLICATE_DISCIPLINE |
| H4 | `PUT` | `/disciplines/{id}` | `updateDiscipline` | Update a discipline | ADMIN | VALIDATION_ERROR, NOT_FOUND, DUPLICATE_DISCIPLINE |
| H5 | `PATCH` | `/disciplines/{id}/status` | `updateDisciplineStatus` | Activate or deactivate a discipline | ADMIN | NOT_FOUND, DISCIPLINE_HAS_ACTIVE_GROUPS |
| H6 | `GET` | `/groups` | `listGroups` | List groups with schedules | ADMIN, COACH | No se declaran códigos específicos |
| H7 | `GET` | `/groups/{id}` | `getGroup` | Get a group | ADMIN, COACH | NOT_FOUND |
| H8 | `POST` | `/groups` | `createGroup` | Create a group | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_MAX_CAPACITY, INVALID_COACH, DISCIPLINE_INACTIVE |
| H9 | `PUT` | `/groups/{id}` | `updateGroup` | Update group name and capacity | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_MAX_CAPACITY, CAPACITY_BELOW_OCCUPIED |
| R1 | `GET` | `/reports/enrollments` | `getEnrollmentReport` | Enrollment indicators | ADMIN | VALIDATION_ERROR |
| R2 | `GET` | `/reports/attendance` | `getAttendanceReport` | Attendance indicators | ADMIN, COACH | VALIDATION_ERROR, INVALID_DATE_RANGE |
| R3 | `GET` | `/reports/attendance/athletes/{id}` | `getAthleteAttendanceReport` | Attendance history and rate of one athlete | ADMIN, COACH, GUARDIAN | NOT_FOUND, INVALID_DATE_RANGE |
| R4 | `GET` | `/reports/attendance/recurrent-absences` | `getRecurrentAbsences` | Athletes with consecutive absences | ADMIN, COACH | VALIDATION_ERROR |
| U1 | `POST` | `/auth/login` | `login` | Log in and get a JWT | PUBLIC | VALIDATION_ERROR, INVALID_CREDENTIALS, USER_INACTIVE |
| U10 | `PATCH` | `/users/{id}/status` | `updateUserStatus` | Activate or deactivate a user | ADMIN | NOT_FOUND, CANNOT_DEACTIVATE_SELF, COACH_HAS_ACTIVE_GROUPS |
| U11 | `GET` | `/coaches` | `listCoaches` | List coaches | ADMIN | No se declaran códigos específicos |
| U12 | `GET` | `/guardians` | `listGuardians` | List guardians (paginated) | ADMIN | No se declaran códigos específicos |
| U2 | `POST` | `/users/register` | `registerGuardian` | Register a guardian account | PUBLIC | VALIDATION_ERROR, DATA_POLICY_NOT_ACCEPTED, EMAIL_ALREADY_REGISTERED, ID_NUMBER_ALREADY_REGISTERED, WEAK_PASSWORD |
| U3 | `GET` | `/users/me` | `getMyProfile` | Get the authenticated user's profile | ADMIN, COACH, GUARDIAN | No se declaran códigos específicos |
| U4 | `PUT` | `/users/me` | `updateMyProfile` | Update the authenticated user's profile | ADMIN, COACH, GUARDIAN | VALIDATION_ERROR |
| U5 | `PUT` | `/users/me/password` | `changeMyPassword` | Change the authenticated user's password | ADMIN, COACH, GUARDIAN | VALIDATION_ERROR, INCORRECT_CURRENT_PASSWORD, WEAK_PASSWORD |
| U6 | `GET` | `/users` | `listUsers` | List users (paginated) | ADMIN | No se declaran códigos específicos |
| U7 | `GET` | `/users/{id}` | `getUser` | Get a user | ADMIN | NOT_FOUND |
| U8 | `POST` | `/users` | `createStaffUser` | Create a coach or an admin account | ADMIN | VALIDATION_ERROR, EMAIL_ALREADY_REGISTERED, ID_NUMBER_ALREADY_REGISTERED, WEAK_PASSWORD |
| U9 | `PUT` | `/users/{id}` | `updateUser` | Update a user | ADMIN | VALIDATION_ERROR, NOT_FOUND, EMAIL_ALREADY_REGISTERED |

## Esquemas reutilizables

### `Error`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `timestamp` | string (date-time) | Sí |
| `status` | integer | Sí |
| `code` | ErrorCode | Sí |
| `message` | string | Sí |
| `details` | array[] | Sí |
| `path` | string | Sí |

### `ErrorDetail`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `field` | string | Sí |
| `message` | string | Sí |

### `PageMetadata`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `page` | integer | Sí |
| `size` | integer | Sí |
| `totalElements` | integer (int64) | Sí |
| `totalPages` | integer | Sí |

### `IdName`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `name` | string | Sí |

### `PersonRef`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `fullName` | string | Sí |

### `AthleteRef`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |

### `StatusRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `status` | EntityStatus | Sí |

### `ReasonRequiredRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `reason` | string | Sí |

### `ReasonOptionalRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `reason` | string | No / no marcado required |

### `LoginRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `email` | string (email) | Sí |
| `password` | string (password) | Sí |

### `LoginResponse`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `accessToken` | string | Sí |
| `tokenType` | string | Sí |
| `expiresInSeconds` | integer | Sí |
| `user` | object | Sí |

### `RegisterGuardianRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `idType` | string | Sí |
| `idNumber` | string | Sí |
| `email` | string (email) | Sí |
| `phone` | string | Sí |
| `homeAddress` | string | No / no marcado required |
| `password` | string (password) | Sí |
| `acceptedDataPolicy` | boolean | Sí |

### `CreateUserRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `role` | string: ADMIN, COACH | Sí |
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `idType` | string | Sí |
| `idNumber` | string | Sí |
| `email` | string (email) | Sí |
| `phone` | string | Sí |
| `password` | string (password) | Sí |
| `specialty` | string | No / no marcado required |
| `yearsOfExperience` | integer | No / no marcado required |

### `UpdateUserRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `email` | string (email) | No / no marcado required |
| `phone` | string | Sí |
| `homeAddress` | string | No / no marcado required |
| `specialty` | string | No / no marcado required |
| `yearsOfExperience` | integer | No / no marcado required |

### `UpdateOwnProfileRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `phone` | string | Sí |
| `homeAddress` | string | No / no marcado required |
| `specialty` | string | No / no marcado required |
| `yearsOfExperience` | integer | No / no marcado required |

### `ChangePasswordRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `currentPassword` | string (password) | Sí |
| `newPassword` | string (password) | Sí |

### `UserProfile`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `idType` | string | Sí |
| `idNumber` | string | Sí |
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `email` | string (email) | Sí |
| `phone` | string | Sí |
| `role` | Role | Sí |
| `status` | EntityStatus | Sí |
| `acceptedDataPolicy` | boolean | No / no marcado required |
| `guardian` | object | No / no marcado required |
| `coach` | object | No / no marcado required |

### `CoachListItem`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `userId` | integer (int32) | Sí |
| `fullName` | string | Sí |
| `specialty` | string | No / no marcado required |
| `yearsOfExperience` | integer | No / no marcado required |
| `status` | EntityStatus | Sí |

### `GuardianListItem`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `userId` | integer (int32) | Sí |
| `fullName` | string | Sí |
| `email` | string (email) | Sí |
| `phone` | string | Sí |

### `CreateAthleteRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `guardianId` | integer (int32) | No / no marcado required |
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `idType` | string | Sí |
| `idNumber` | string | Sí |
| `birthDate` | string (date) | Sí |
| `gender` | string | No / no marcado required |

### `UpdateAthleteRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `idType` | string | Sí |
| `idNumber` | string | Sí |
| `birthDate` | string (date) | Sí |
| `gender` | string | No / no marcado required |

### `Athlete`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `guardianId` | integer (int32) | Sí |
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `idType` | string | Sí |
| `idNumber` | string | Sí |
| `birthDate` | string (date) | Sí |
| `age` | integer | Sí |
| `gender` | string | No / no marcado required |
| `hasActiveEnrollment` | boolean | Sí |

### `GroupAvailability`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `groupId` | integer (int32) | Sí |
| `groupName` | string | Sí |
| `discipline` | object | Sí |
| `coach` | PersonRef | Sí |
| `schedules` | array[] | Sí |
| `maxCapacity` | integer | Sí |
| `occupiedSpots` | integer | Sí |
| `availableSpots` | integer | Sí |

### `CreateEnrollmentRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `athleteId` | integer (int32) | Sí |
| `groupId` | integer (int32) | Sí |
| `period` | Period | Sí |

### `EnrollmentSummary`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `athlete` | AthleteRef | Sí |
| `group` | IdName | Sí |
| `discipline` | IdName | Sí |
| `period` | Period | Sí |
| `status` | EnrollmentStatus | Sí |
| `paymentStatus` | PaymentStatus | Sí |
| `paymentReported` | boolean | Sí |
| `createdAt` | string (date-time) | Sí |

### `ConfirmEnrollmentRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `groupId` | integer (int32) | No / no marcado required |
| `payment` | RegisterPaymentRequest | No / no marcado required |

### `ChangeGroupRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `groupId` | integer (int32) | Sí |

### `PaymentReportRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `paymentDate` | string (date) | No / no marcado required |
| `notes` | string | No / no marcado required |

### `RegisterPaymentRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `amountPaid` | number (decimal 10,2; múltiplo de 0.01) | Sí |
| `paymentDate` | string (date) | Sí |
| `receiptReference` | string | No / no marcado required |
| `notes` | string | No / no marcado required |

### `PaymentStatusRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `paymentStatus` | string: OVERDUE | Sí |

### `Payment`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `enrollmentId` | integer (int32) | Sí |
| `athlete` | AthleteRef | No / no marcado required |
| `discipline` | IdName | No / no marcado required |
| `period` | Period (mes de `paymentDate`) | No / no marcado required |
| `amountPaid` | number (decimal 10,2; múltiplo de 0.01) | Sí |
| `paymentDate` | string (date) | Sí |
| `receiptReference` | string | No / no marcado required |
| `notes` | string | No / no marcado required |
| `registeredBy` | PersonRef | No / no marcado required |
| `createdAt` | string (date-time) | No / no marcado required |

### `DisciplineRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `name` | string | Sí |
| `description` | string | No / no marcado required |
| `monthlyFee` | number (decimal 10,2; múltiplo de 0.01) | Sí |

### `Discipline`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `name` | string | Sí |
| `description` | string | No / no marcado required |
| `monthlyFee` | number (decimal 10,2; múltiplo de 0.01) | Sí |
| `status` | EntityStatus | Sí |

### `Schedule`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `dayOfWeek` | DayOfWeek | Sí |
| `startTime` | Time | Sí |
| `endTime` | Time | Sí |

### `ScheduleRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `dayOfWeek` | DayOfWeek | Sí |
| `startTime` | Time | Sí |
| `endTime` | Time | Sí |

### `Group`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `name` | string | Sí |
| `discipline` | IdName | Sí |
| `coach` | PersonRef | Sí |
| `maxCapacity` | integer | Sí |
| `status` | EntityStatus | Sí |
| `schedules` | array[] | Sí |

### `CreateGroupRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `disciplineId` | integer (int32) | Sí |
| `coachId` | integer (int32) | Sí |
| `name` | string | Sí |
| `maxCapacity` | integer | Sí |

### `UpdateGroupRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `name` | string | Sí |
| `maxCapacity` | integer | Sí |

### `AssignCoachRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `coachId` | integer (int32) | Sí |

### `CreateSessionRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `groupId` | integer (int32) | Sí |
| `date` | string (date) | Sí |
| `startTime` | Time | Sí |
| `endTime` | Time | Sí |
| `notes` | string | No / no marcado required |

### `UpdateSessionRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `date` | string (date) | Sí |
| `startTime` | Time | Sí |
| `endTime` | Time | Sí |
| `notes` | string | No / no marcado required |

### `GenerateSessionsRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `groupId` | integer (int32) | Sí |
| `dateFrom` | string (date) | Sí |
| `dateTo` | string (date) | Sí |
| `notify` | boolean | No / no marcado required |

### `TrainingSession`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `group` | IdName | Sí |
| `discipline` | IdName | Sí |
| `coach` | PersonRef | Sí |
| `date` | string (date) | Sí |
| `startTime` | Time | Sí |
| `endTime` | Time | Sí |
| `status` | SessionStatus | Sí |
| `attendanceRecorded` | boolean | Sí |
| `notes` | string | No / no marcado required |
| `athletes` | array[] | No / no marcado required |

### `GenerateSessionsResponse`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `created` | integer | Sí |
| `skipped` | integer | Sí |
| `sessions` | array[] | Sí |

### `AttendanceRosterEntry`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `athleteId` | integer (int32) | Sí |
| `firstNames` | string | Sí |
| `lastNames` | string | Sí |
| `status` | object | Sí |

### `AttendanceRoster`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `sessionId` | integer (int32) | Sí |
| `group` | IdName | Sí |
| `date` | string (date) | Sí |
| `status` | SessionStatus | Sí |
| `attendanceRecorded` | boolean | Sí |
| `records` | array[] | Sí |

### `RecordAttendanceRequest`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `records` | array[] | Sí |

### `AttendanceRecord`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `id` | integer (int32) | Sí |
| `sessionId` | integer (int32) | Sí |
| `date` | string (date) | Sí |
| `group` | IdName | Sí |
| `discipline` | IdName | Sí |
| `athlete` | AthleteRef | Sí |
| `status` | AttendanceStatus | Sí |
| `recordedAt` | string (date-time) | Sí |

### `EnrollmentReport`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `period` | Period | Sí |
| `totalRequests` | integer | Sí |
| `byStatus` | object | Sí |
| `completionRate` | number (double) | Sí |
| `abandonmentRate` | number (double) | Sí |
| `averageConfirmationHours` | number (double) | Sí |
| `byDiscipline` | array[] | Sí |
| `payments` | object | Sí |

### `AttendanceReport`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `scheduledSessions` | integer | Sí |
| `completedSessions` | integer | Sí |
| `recordingCoverage` | number (double) | Sí |
| `attendanceRate` | number (double) | Sí |
| `byGroup` | array[] | Sí |

### `AthleteAttendanceReport`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `athleteId` | integer (int32) | Sí |
| `recordedSessions` | integer | Sí |
| `present` | integer | Sí |
| `absent` | integer | Sí |
| `attendanceRate` | number (double) | Sí |
| `byGroup` | array[] | Sí |

### `RecurrentAbsence`

| Campo | Tipo / referencia | Obligatorio |
|---|---|---|
| `athlete` | AthleteRef | Sí |
| `group` | IdName | Sí |
| `consecutiveAbsences` | integer | Sí |
| `lastAttendance` | string (date) | No / no marcado required |

## Observaciones del contrato

- El archivo recibido tenía una segunda copia del documento pegada al final de la definición `RecurrentAbsence`, lo que impedía parsearlo. Se incluye en este paquete una copia saneada que conserva la primera especificación completa y fue parseada correctamente. Conserva el archivo original fuera de este paquete hasta aprobar el cambio.
- Los identificadores de endpoints no son consecutivos en algunos grupos (por ejemplo, faltan E9–E11 en el inventario visible). No renumerarlos automáticamente; pueden reservarse para la documentación de la matrícula (ver abajo).
- Los torneos están fuera del alcance (Definición del Proyecto v02): el contrato no debe incluir rutas `/tournaments` y las tablas del MER se retiran.

## Ampliaciones pendientes del contrato
Requeridas por los documentos oficiales v02 y aún no declaradas en `openapi.yaml`. Deben diseñarse y aprobarse antes de implementarse:
- Proceso 3: solicitud de recuperación de contraseña y restablecimiento con token (públicos, bajo `/auth`).
- Proceso 3: reporte de indicadores de usuarios (cuentas por rol y periodo, porcentaje de acudientes con deportistas vinculados, cuentas inactivas), solo `ADMIN`.
- Proceso 3: código de error para un token vigente de una cuenta inactivada.
- Proceso 1: carga, consulta y validación de los documentos de la matrícula (autorización del acudiente y certificado médico si aplica), y su efecto sobre la confirmación (E12).
- Proceso 1: consulta consolidada del estado de pago por deportista.
- Proceso 1: renovación de la matrícula por periodo, si se decide modelarla explícitamente.

`R1` (indicadores del Proceso 1) ya está contratado y pendiente de implementación.
- El contrato declara algunas reglas detalladas en descripciones. Mantenerlas también en pruebas de servicio, no solo en documentación.
