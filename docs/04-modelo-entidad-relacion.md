# 04. Modelo entidad-relación (MER)

## Fuente
`docs/MER.pdf`, diagrama generado con dbdiagram.io. Este documento describe las entidades del diagrama y su finalidad. Para nombres, tipos, nulabilidad, claves y restricciones exactas, el diagrama es la referencia normativa y las entidades JPA deben seguirlo al pie de la letra. No inferir una restricción `UNIQUE`, `NOT NULL` o `ON DELETE` si no aparece explícitamente.

No se usan migraciones versionadas: el esquema se deriva de las entidades JPA, que mapean este modelo.

## Entidades identificadas

| Entidad | Responsabilidad / observaciones |
|---|---|
| `roles` | Catálogo de roles asignados a usuarios; contiene identificador, nombre (único) y descripción. |
| `users` | Cuenta de acceso y datos comunes: rol, tipo y número de documento de identidad, nombres, apellidos, correo, hash de contraseña, teléfono, aceptación de la política de datos y su fecha, estado y fecha de creación. Correo y número de documento son únicos. |
| `guardians` | Perfil específico de acudiente asociado a un usuario (1:1, `user_id` único); incluye dirección. |
| `coaches` | Perfil de entrenador asociado a un usuario (1:1, `user_id` único); incluye especialidad y años de experiencia. No tiene columna de estado: el estado del entrenador es `users.status`. |
| `athletes` | Datos del deportista: acudiente asociado, tipo y número de documento de identidad (único), nombres, apellidos, fecha de nacimiento, género y fecha de creación. |
| `disciplines` | Catálogo de disciplinas con nombre (único), descripción, mensualidad y estado. |
| `groups` / `training_groups` | Grupo de entrenamiento, disciplina, entrenador, nombre, capacidad máxima y estado. El OpenAPI indica que el nombre físico en MySQL 8 debe ser `training_groups`. |
| `schedules` | Horarios semanales asociados a un grupo, con día y hora de inicio/fin. |
| `enrollments` | Matrícula del deportista a un grupo desde un mes de inicio (`period_month`), vigente mes a mes hasta su rechazo o cancelación; incluye estado, estado de pago, notas de revisión, fecha de confirmación, reporte de pago (indicador, fecha y nota) y fecha de creación. |
| `payment_history` | Historial de pagos asociados a matrícula (varios por matrícula, como máximo uno por mes calendario de `payment_date`), con usuario administrador que registra, monto, fecha, referencia de recibo, notas y creación. |
| `training_sessions` | Sesiones programadas de grupo, con entrenador, fecha, horarios, estado y notas. |
| `attendance_records` | Marca de asistencia asociada a sesión y deportista, con fechas de creación y registro; la operación A8 permite sobrescribir marcas de una sesión. |
| `notifications` | Registro de notificaciones asociadas a un usuario: tipo, asunto, estado de entrega, intentos y fecha de creación. |
| `tournaments` | Torneos internos asociados a disciplina y administrador creador, con nombre, categoría, reglas, modalidad, fechas y estado. |
| `tournament_teams` | Equipos dentro de un torneo, con nombre. |
| `tournament_registrations` | Inscripciones de deportistas a torneos, con equipo opcional, estado y fecha de creación. |
| `tournament_matches` | Partidos de un torneo, con fecha, lugar, equipos o deportistas local/visitante (opcionales), marcadores y estado. |

## Tipos y convenciones de mapeo
- Las claves primarias y foráneas son `INT` en el MER y se mapean como `Integer` en las entidades JPA.
- Los montos (`monthly_fee`, `amount_paid`) son `DECIMAL(10,2)` y se mapean como `BigDecimal`.
- Las longitudes de `VARCHAR` del diagrama se respetan en las entidades y en la validación de los DTO.

## Relaciones principales
- `roles` 1:N `users`.
- `users` 1:0..1 `guardians` y 1:0..1 `coaches`, según rol.
- `guardians` 1:N `athletes`.
- `disciplines` 1:N grupos.
- `coaches` 1:N grupos; cada grupo tiene un entrenador asignado.
- `groups` 1:N `schedules` y 1:N `training_sessions`.
- `athletes`, `groups` y periodo se relacionan mediante `enrollments`.
- `enrollments` 1:N `payment_history`; `users` (administrador) 1:N `payment_history`.
- `training_sessions` 1:N `attendance_records`; cada registro referencia un deportista.
- `users` 1:N `notifications`.
- `tournaments` se relaciona con `tournament_teams`, `tournament_registrations` y `tournament_matches`.

## Reglas de implementación
1. Verificar los nombres y tipos reales contra el diagrama antes de generar entidades JPA.
2. No exponer entidades JPA directamente desde controladores; definir DTO de API.
3. Usar claves foráneas y restricciones de base de datos para preservar integridad.
4. Evitar cascadas de borrado no aprobadas, especialmente en datos de menores, matrículas, pagos y asistencia.
5. Definir índices sobre claves foráneas y filtros usados frecuentemente tras revisar consultas reales.
6. Para disponibilidad y cupos, aplicar transacción y bloqueo/concurrencia según el contrato de E6.
7. La nomenclatura de API no obliga a que el nombre físico de tabla sea igual; documentar mapeos explícitos, en particular `groups` → `training_groups`.

## Decisiones (2026-10-11)
- Las entidades siguen el MER al pie de la letra, incluidos los tipos `INT` de los identificadores.
- El estado del entrenador se toma de `users.status`.
- El `period_month` de una matrícula es su mes de inicio. Una matrícula puede tener varios pagos, como máximo uno por mes calendario; el mes que cubre un pago es el de su `payment_date` y el más reciente es el último pago.
- El sistema no gestiona soportes documentales adjuntos (certificado médico ni autorización del acudiente). Las columnas `enrollments.medical_certificate_path` y `enrollments.guardian_authorization_path` del diagrama no se implementan; el diagrama se actualizará para retirarlas.

## Discrepancias / verificaciones pendientes
- El contrato API actual no incluye rutas de torneos aunque las tablas existen en el MER.
- Verificar si las notificaciones se persisten antes o después del envío de correo y cómo se registran fallos/reintentos.
- La regla de matrícula duplicada (una sola matrícula vigente por deportista y disciplina) depende del estado y de la disciplina del grupo, por lo que no es una restricción única de columnas: se valida en el servicio de matrículas dentro de la transacción que reserva el cupo.
- La regla de un pago por mes se deriva del mes de `payment_date`, que no es una columna propia; se valida en el servicio de pagos y no con una restricción única de base de datos.
- Alineado (2026-10-11): el OpenAPI declara los identificadores como `int32` y los montos como decimales de dos cifras, conforme al MER.
- No asumir que la tabla `groups` es el nombre físico: el OpenAPI especifica `training_groups` para MySQL 8.
