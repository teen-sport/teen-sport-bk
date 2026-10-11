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
| E12 | `POST` | `/enrollments/{id}/confirm` | `confirmEnrollment` | Confirm an enrollment | ADMIN | VALIDATION_ERROR, NOT_FOUND, INVALID_STATE, GROUP_INACTIVE, GROUP_OTHER_DISCIPLINE, NO_AVAILABLE_SPOTS, INSUFFICIENT_AMOUNT, FUTURE_DATE |
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
| H10 | `PATCH` | `/groups/{id}/status` | `updateGroupStatus` | Activate or deactivate a group | ADMIN | NOT_FOUND, GROUP_HAS_ACTIVITY |
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
- Los identificadores de endpoints no son consecutivos en algunos grupos (por ejemplo, faltan E9–E11 en el inventario visible). No renumerarlos automáticamente; verificar si corresponden a requisitos omitidos o IDs reservados.
- El MER contempla tablas de torneos, pero esta especificación no incluye rutas `/tournaments` ni operaciones equivalentes.
- El contrato declara algunas reglas detalladas en descripciones. Mantenerlas también en pruebas de servicio, no solo en documentación.
