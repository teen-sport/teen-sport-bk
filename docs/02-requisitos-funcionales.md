# 02. Requisitos funcionales

> Esta lista se organiza por los tres procesos de negocio de la Definición del Proyecto (v03, 9/11/2026) y resume el comportamiento que se desprende de esos documentos y del OpenAPI. Los identificadores `x-endpoint-id` se conservan como trazabilidad del contrato. Los requisitos marcados **Pendiente de implementar** los exigen los documentos oficiales pero aún no existen en el contrato ni en el código.

## Proceso 3 — Registro, autenticación y gestión de usuarios

### Autenticación y cuentas
- `U1`: autenticar por correo/credenciales y devolver JWT; error genérico para correo o contraseña incorrectos.
- `U2`: registrar una cuenta de acudiente (autorregistro), con aceptación obligatoria de la política de tratamiento de datos; crear usuario y acudiente en una transacción. Datos personales: nombres, apellidos, documento de identidad, correo y teléfono.
- `U3–U5`: consultar/actualizar perfil propio y cambiar contraseña; no permitir cambiar correo, documento o rol mediante la actualización del perfil. La actualización es un reemplazo completo: los campos de otro rol se ignoran y un campo opcional del rol no enviado queda vacío. El cambio de contraseña exige la contraseña actual; los tokens ya emitidos siguen vigentes hasta su vencimiento.
- `U6–U12`: administración de usuarios, consulta individual, creación de personal, cambio de estado, listado de entrenadores y acudientes. Un administrador no puede desactivar su propia cuenta; la edición no cambia el rol ni el documento y conserva el correo si no se envía uno nuevo. La activación e inactivación (U10) aplica a cuentas de cualquier rol, incluidos los acudientes.
- Los roles `COACH` y `ADMIN` los crea un administrador; el registro público está limitado a acudientes.
- Unicidad: el documento de identidad y el correo deben ser únicos (RNF-11). Un intento duplicado, incluso concurrente, se rechaza con un error controlado (`409 EMAIL_ALREADY_REGISTERED` o `409 ID_NUMBER_ALREADY_REGISTERED`), nunca con `500` (desde la Entrega 3, etapa 2).
- Cierre de sesión: el frontend descarta el token; el backend no mantiene sesiones.
- **Pendiente de implementar — Recuperación de contraseña por correo** (Definición P3, SAD 1.3–1.4, ADR-07; se construye después de la integración de correo, decisión del 2026-11-09): solicitar la recuperación con el correo y restablecer la contraseña con un token de un solo uso y vigencia limitada. La respuesta de la solicitud no debe revelar si el correo existe. La nueva contraseña cumple la misma política que el registro. Requiere contrato en OpenAPI, tabla en el MER y el envío por correo (ver ADR-009).
- **Mejora futura — Rechazo de cuentas inactivas con token vigente** (posible mejora futura (decisión del 2026-11-09); no es prioridad): una cuenta inactivada no debe poder seguir operando con un token emitido antes de la inactivación (Plan de Calidad 3.3.3, "casos de acceso al sistema"; RNF-13).
- **Pendiente de implementar — Notificación de registro exitoso** por correo (salida del P3; RNF-14). Al final de la Entrega 3, con el adaptador de correo.

### Perfiles de deportistas (vinculados al acudiente)
- `E1–E4`: registrar, listar, consultar y actualizar deportistas. La edad se calcula con la fecha local de la academia (`America/Bogota`); al editar solo se valida si cambia la fecha de nacimiento. Un administrador solo puede registrar deportistas para un acudiente activo.
- La edad debe estar entre 12 y 17 años en la fecha de la solicitud, según contrato.
- Cada perfil queda vinculado al acudiente que lo registró, sin perfiles huérfanos ni vinculaciones erróneas (RNF-12). Los acudientes solo pueden gestionar deportistas asociados a su cuenta; el administrador puede operar en nombre de un acudiente con las reglas documentadas.
- El deportista no es usuario ni rol del sistema.
- **Mejora futura — Inactivación del deportista** (posible mejora futura (decisión del 2026-11-09); cambia `openapi.yaml`): la solicitud de supresión de datos de un menor se atiende inactivando su perfil, no borrándolo, para conservar el historial de matrículas, pagos y asistencia. Requiere un estado en `athletes` (cambio pendiente del MER) y una operación en el contrato.
- No existe DELETE de deportistas en el contrato; la salida de la academia se representa mediante la gestión de la matrícula.

### Indicadores del Proceso 3
- **Pendiente de implementar, Entrega 3, etapa 4** (KPI de la Definición P3): número de cuentas registradas por rol y periodo, porcentaje de acudientes con deportistas vinculados y número de cuentas inactivas. Requiere un reporte nuevo en el contrato (solo `ADMIN`).

## Proceso 1 — Gestión de Inscripción y Matrícula

### Disciplinas, grupos y horarios (soporte de cupos y horarios)
- `H1–H5`: listar, consultar, crear, actualizar y activar/desactivar disciplinas. La mensualidad debe ser positiva. No se puede desactivar una disciplina con grupos activos. El nombre es único sin distinguir mayúsculas y quien no es administrador solo ve disciplinas activas.
- `H6–H11`: listar, consultar, crear, actualizar, activar/desactivar grupos y asignar entrenador.
- La capacidad de grupo está entre 1 y 15. El entrenador y la disciplina deben estar activos al crear el grupo. El grupo se almacena en la tabla `groups` (la del MER y la base desplegada). Las franjas no pueden cruzarse dentro del grupo ni con las de otros grupos activos del entrenador; las franjas contiguas sí se permiten.
- No reducir la capacidad por debajo de las matrículas vigentes (`PENDING_REVIEW` y `ACTIVE`) del grupo.
- `H12–H14`: crear, actualizar y eliminar horarios del grupo conforme a las reglas del contrato.

### Disponibilidad y matrículas
- `E5`: consultar grupos activos de disciplinas activas y disponibilidad para un periodo.
- El `period` de una matrícula es su mes de inicio; la matrícula sigue vigente mes a mes hasta ser rechazada o cancelada.
- Los cupos ocupados de un mes cuentan las matrículas `PENDING_REVIEW` y `ACTIVE` con mes de inicio igual o anterior. Como cada matrícula vigente ocupa cupo en todos los meses siguientes, para reservar (E6) se compara la capacidad con todas las matrículas vigentes del grupo, y E5 informa como disponibles solo los cupos que aún se pueden reservar.
- `E6`: solicitar matrícula; estado inicial `PENDING_REVIEW`, pago inicial `OVERDUE` y reserva de cupo bajo bloqueo pesimista del grupo.
- Un deportista no puede tener dos matrículas vigentes (`PENDING_REVIEW` o `ACTIVE`) en la misma disciplina, sin importar grupo ni mes de inicio (`DUPLICATE_ENROLLMENT`); sí en disciplinas distintas.
- Si no hay cupos, no se crea matrícula y se devuelve conflicto. La notificación por correo de esta situación se integra con el adaptador de correo.
- `E7–E8`: listar matrículas y consultar detalle con horarios, aplicando permisos de propiedad.
- `E12`: administrador confirma matrícula, puede reasignar grupo de la misma disciplina y registrar un pago en la misma operación. Si no registra pago, la matrícula queda `OVERDUE`.
- `E13`: administrador rechaza matrícula pendiente y libera cupo.
- `E14`: cancelar matrícula conforme al rol y estado; liberar cupo cuando corresponda.
- `E15`: cambiar de grupo dentro de la misma disciplina, conservando el mes de inicio; liberar el cupo anterior y reservar el nuevo sin modificar sesiones pasadas ni asistencia.
- **Fuera del alcance actual — Documentación de la matrícula** (decisión del 2026-11-09): la Definición v03 ya no incluye "validar la documentación" entre las actividades del P1, aunque sus entradas aún mencionan "documentos requeridos" y el SAD 1.4 la "validación de documentación". El equipo decidió no implementarla por ahora. Los IDs `E9–E11` siguen reservados y las columnas de documentos de `enrollments` no se mapean.
- **Renovación por periodo — resuelto (2026-11-09)**: se mantiene la matrícula continua mes a mes con pago mensual como interpretación de la "matrícula por periodo" de la Definición. No se modela una renovación explícita.
- **Pendiente de implementar — Notificación de inscripción exitosa** por correo (salida del P1). Al final de la Entrega 3, con el adaptador de correo.

### Pagos presenciales
- `E16`: el acudiente reporta que pagó presencialmente; solo activa `paymentReported`, no cambia el estado financiero.
- `E17`: el administrador registra el pago presencial; el monto debe ser al menos la mensualidad vigente y se actualiza a `UP_TO_DATE`. Una matrícula puede tener varios pagos, como máximo uno por mes calendario de `paymentDate`; el pago con `paymentDate` más reciente es el último.
- `E18`: el administrador puede marcar la matrícula `OVERDUE`; `UP_TO_DATE` solo se alcanza registrando un pago. No hay proceso automático de actualización.
- `E19`: consultar historial de pagos con filtrado de pertenencia para acudientes.
- El estado de pago se lleva por matrícula (la mensualidad es por disciplina). La Definición habla del estado de pago "de cada deportista": E7 con el filtro `athleteId` devuelve el `paymentStatus` de cada matrícula del deportista; se propone darlo por cubierto sin endpoint nuevo (pendiente de confirmar).
- No implementar pasarela de pagos en línea.

### Indicadores del Proceso 1
- `R1` (Entrega 3, etapa 3): informe de matrículas. Cubre los KPI de la Definición P1: número de inscripciones por periodo, tiempo promedio del proceso de inscripción (`averageConfirmationHours`) y porcentaje de matrículas completas frente a abandonadas (`completionRate`, `abandonmentRate`). **Implementado (2026-11-10), pendiente de revisión.** Las matrículas del periodo son las de ese mes de inicio; los pagos, los de las matrículas activas vigentes en el periodo.

## Proceso 2 — Gestión de Entrenamientos y Asistencia

### Sesiones y asistencia
- `A1–A6`: crear, listar, generar, consultar, actualizar y cancelar sesiones, con acceso limitado a grupos asignados cuando el actor sea entrenador.
- `A7`: consultar lista de deportistas elegibles para asistencia de una sesión. **Pendiente de implementar** (decisión del 2026-11-09): el entrenador debe ver también la edad del deportista y el contacto del acudiente (nombre y teléfono) para emergencias; requiere ampliar el contrato.
- `A8`: registrar la asistencia completa del grupo en una sola llamada; guardar de nuevo sobrescribe marcas anteriores (upsert). La sesión pasa a `COMPLETED`; no se puede registrar asistencia en una sesión futura o cancelada.
- `A9`: consultar historial paginado de asistencia; administradores ven todo, entrenadores sus grupos y acudientes sus deportistas.
- Notificación de horarios y novedades a los acudientes por correo: al final de la Entrega 3.

### Indicadores del Proceso 2
Entrega 3, etapa 3. **Implementados (2026-11-10), pendientes de revisión.** Reglas de fechas y alcance en `docs/10-plan-implementacion.md`, etapa 3.
- `R2`: informe de asistencia por alcance autorizado.
- `R3`: informe de asistencia de un deportista, con verificación de propiedad.
- `R4`: consulta de ausencias consecutivas, con umbral configurable y filtrado de grupos según rol.

## Fuera de alcance
- Torneos y eventos deportivos, internos o externos: las tablas `tournaments`, `tournament_teams`, `tournament_registrations` y `tournament_matches` del MER se retiran y no deben implementarse.
- Pasarela de pagos, nómina e inventario.

## Contradicciones en los documentos oficiales (pendientes de resolver con los responsables)
- SAD 1.4 incluye "deportista" en la asignación de roles; la Definición establece que el deportista no es usuario. Se aplican los tres roles `ADMIN`, `COACH` y `GUARDIAN`.
- La Definición v03 lista "Deportista" entre los actores involucrados del P3; se entiende como actor de negocio, no como usuario.
- El ADR-05 del SAD v03 aún menciona "eventos y resultados" en la persistencia. Prevalece la exclusión de los torneos.
- Las entradas del P1 (Definición v03) y el SAD 1.4 mencionan la documentación de la matrícula, que el equipo dejó fuera del alcance actual (2026-11-09).
- Plan de Calidad: la sección 3.4.3 fija la meta de notificaciones solo para el registro exitoso y RNF-14 para el registro y la recuperación de contraseña; ambas en ≥ 95 %. Se toma RNF-14.

Resueltas en la v03 (9/11/2026): la meta de notificaciones de 80 % frente a 95 %, los títulos de torneos en la tabla de contenido del Plan y las menciones de torneos en `ReportController` y `ReportService` del SAD.

## Pendientes de definición
- Catálogo de eventos de notificación, persistencia en `notifications`, reintentos y relación entre notificación y envío de correo: se define al llegar a la Entrega 3, etapa 6.
- Versión de la política de datos aceptada: se decide al final del desarrollo; hoy solo se guarda la aceptación y su fecha.

## Tratamiento de datos de menores (decisiones del 2026-11-09)
- La autorización del acudiente al registrarse (U2) cubre el tratamiento de los datos de todos los deportistas que vincule, también los que el administrador registre en su nombre. No se pide una autorización por deportista.
- La supresión de datos de un menor se atenderá inactivando su perfil, como mejora futura (ver Perfiles de deportistas). Mientras no exista, el sistema no la gestiona.
- El entrenador ve la edad del deportista y el contacto del acudiente en la lista de asistencia (ver A7), solo para los deportistas de sus grupos.
- **Posible implementación futura, fuera del alcance actual**: auditoría de quién consulta los datos de menores.
