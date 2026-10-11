# 04. Modelo entidad-relación (MER)

## Fuente
`MER.AC.pdf`, diagrama generado con dbdiagram.io. Este documento describe las entidades visibles en el diagrama y su finalidad. Para nombres, tipos, nulabilidad, claves y restricciones exactas, el diagrama y las migraciones SQL aprobadas son la referencia normativa. No inferir una restricción `UNIQUE`, `NOT NULL` o `ON DELETE` si no aparece explícitamente.

## Entidades identificadas

| Entidad | Responsabilidad / observaciones |
|---|---|
| `roles` | Catálogo de roles asignados a usuarios; contiene identificador y nombre. |
| `users` | Cuenta de acceso y datos comunes: rol, identificación, nombre, correo, hash de contraseña, teléfono, aceptación de política, estado y fechas de creación. |
| `guardians` | Perfil específico de acudiente asociado a usuario; incluye dirección. |
| `coaches` | Perfil de entrenador asociado a usuario; incluye especialidad, años de experiencia y estado. |
| `athletes` | Datos del deportista: acudiente asociado, documento, nombre, nacimiento, género, fechas y referencias a soportes médicos/autorización. |
| `disciplines` | Catálogo de disciplinas con nombre, descripción, mensualidad y estado. |
| `groups` / `training_groups` | Grupo de entrenamiento, disciplina, entrenador, nombre, capacidad y estado. El OpenAPI indica que el nombre físico en MySQL 8 debe ser `training_groups`. |
| `schedules` | Horarios semanales asociados a un grupo, con día y hora de inicio/fin. |
| `enrollments` | Matrícula del deportista a disciplina/grupo por periodo; incluye estado, pago, revisión, fechas y reporte de pago. |
| `payment_history` | Historial de pagos asociados a matrícula, con usuario administrador que registra, monto, fecha, referencia, notas y creación. |
| `training_sessions` | Sesiones programadas de grupo, con fecha, horarios, estado y notas. |
| `attendance_records` | Marca de asistencia asociada a sesión y deportista; la operación A8 permite sobrescribir marcas de una sesión. |
| `notifications` | Registro de notificaciones asociadas a usuario y/o deportista, con contenido/estado y fecha según diagrama. Verificar todos los campos en el archivo original. |
| `tournaments` | Torneos internos asociados a disciplina y administrador creador, con nombre, categoría, reglas, modalidad, fechas y estado. |
| `tournament_teams` | Equipos dentro de un torneo; el diagrama incluye entrenador y relaciones con torneo. |
| `tournament_registrations` | Inscripciones de deportistas y/o equipos a torneos. Confirmar cardinalidades y reglas de unicidad. |
| `tournament_matches` | Partidos asociados a torneo y equipos local/visitante, con fecha y resultados/estado según diagrama. |

## Relaciones principales
- `roles` 1:N `users`.
- `users` puede tener un perfil `guardians` o `coaches`, según rol.
- `guardians` 1:N `athletes`.
- `disciplines` 1:N grupos.
- `coaches` puede estar asociado a varios grupos; cada grupo tiene un entrenador asignado según contrato.
- `groups` 1:N `schedules` y 1:N `training_sessions`.
- `athletes`, `groups` y periodo se relacionan mediante `enrollments`.
- `enrollments` se relaciona con `payment_history` y con la elegibilidad del deportista para sesiones/asistencia.
- `training_sessions` 1:N `attendance_records`; cada registro referencia un deportista.
- `tournaments` se relaciona con `tournament_teams`, `tournament_registrations` y `tournament_matches`.

## Reglas de implementación
1. Verificar los nombres y tipos reales contra el diagrama antes de generar entidades JPA.
2. No exponer entidades JPA directamente desde controladores; definir DTO de API.
3. Usar claves foráneas y restricciones de base de datos para preservar integridad.
4. Evitar cascadas de borrado no aprobadas, especialmente en datos de menores, matrículas, pagos y asistencia.
5. Definir índices sobre claves foráneas y filtros usados frecuentemente tras revisar consultas reales.
6. Para disponibilidad y cupos, aplicar transacción y bloqueo/concurrencia según el contrato de E6.
7. La nomenclatura de API no obliga a que el nombre físico de tabla sea igual; documentar mapeos explícitos, en particular `groups` → `training_groups`.

## Discrepancias / verificaciones pendientes
- El contrato API actual no incluye rutas de torneos aunque las tablas existen en el MER.
- Verificar si las notificaciones se persisten antes o después del envío de correo y cómo se registran fallos/reintentos.
- Confirmar restricciones únicas (correo, documento, inscripción por deportista/disciplina/periodo, pago por matrícula) en el esquema físico y migraciones.
- No asumir que la tabla `groups` es el nombre físico: el OpenAPI especifica `training_groups` para MySQL 8.
