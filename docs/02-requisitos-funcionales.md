# 02. Requisitos funcionales

> Esta lista se organiza por los tres procesos de negocio de la Definición del Proyecto (v02) y resume el comportamiento que se desprende de esos documentos y del OpenAPI. Los identificadores `x-endpoint-id` se conservan como trazabilidad del contrato. Los requisitos marcados **Pendiente de implementar** los exigen los documentos oficiales pero aún no existen en el contrato ni en el código.

## Proceso 3 — Registro, autenticación y gestión de usuarios

### Autenticación y cuentas
- `U1`: autenticar por correo/credenciales y devolver JWT; error genérico para correo o contraseña incorrectos.
- `U2`: registrar una cuenta de acudiente (autorregistro), con aceptación obligatoria de la política de tratamiento de datos; crear usuario y acudiente en una transacción. Datos personales: nombres, apellidos, documento de identidad, correo y teléfono.
- `U3–U5`: consultar/actualizar perfil propio y cambiar contraseña; no permitir cambiar correo, documento o rol mediante la actualización del perfil. La actualización es un reemplazo completo: los campos de otro rol se ignoran y un campo opcional del rol no enviado queda vacío. El cambio de contraseña exige la contraseña actual; los tokens ya emitidos siguen vigentes hasta su vencimiento.
- `U6–U12`: administración de usuarios, consulta individual, creación de personal, cambio de estado, listado de entrenadores y acudientes. Un administrador no puede desactivar su propia cuenta; la edición no cambia el rol ni el documento y conserva el correo si no se envía uno nuevo. La activación e inactivación (U10) aplica a cuentas de cualquier rol, incluidos los acudientes.
- Los roles `COACH` y `ADMIN` los crea un administrador; el registro público está limitado a acudientes.
- Unicidad: el documento de identidad y el correo deben ser únicos (RNF-11). Un intento duplicado, incluso concurrente, debe rechazarse con un error controlado (`409`), nunca con `500`.
- Cierre de sesión: el frontend descarta el token; el backend no mantiene sesiones.
- **Pendiente de implementar — Recuperación de contraseña por correo** (Definición P3, SAD 1.3–1.4, ADR-07): solicitar la recuperación con el correo y restablecer la contraseña con un token de un solo uso y vigencia limitada. La respuesta de la solicitud no debe revelar si el correo existe. La nueva contraseña cumple la misma política que el registro. Requiere contrato en OpenAPI, tabla en el MER y el envío por correo (ver ADR-009).
- **Pendiente de implementar — Rechazo de cuentas inactivas con token vigente**: una cuenta inactivada no debe poder seguir operando con un token emitido antes de la inactivación (Plan de Calidad 3.3.3, "casos de acceso al sistema"; RNF-13).
- **Pendiente de implementar — Notificación de registro exitoso** por correo (salida del P3; RNF-14). Fuera de esta entrega; se integra con el adaptador de correo.

### Perfiles de deportistas (vinculados al acudiente)
- `E1–E4`: registrar, listar, consultar y actualizar deportistas. La edad se calcula con la fecha local de la academia (`America/Bogota`); al editar solo se valida si cambia la fecha de nacimiento. Un administrador solo puede registrar deportistas para un acudiente activo.
- La edad debe estar entre 12 y 17 años en la fecha de la solicitud, según contrato.
- Cada perfil queda vinculado al acudiente que lo registró, sin perfiles huérfanos ni vinculaciones erróneas (RNF-12). Los acudientes solo pueden gestionar deportistas asociados a su cuenta; el administrador puede operar en nombre de un acudiente con las reglas documentadas.
- El deportista no es usuario ni rol del sistema.
- No existe DELETE de deportistas en el contrato; la salida de la academia se representa mediante la gestión de la matrícula.

### Indicadores del Proceso 3
- **Pendiente de implementar** (KPI de la Definición P3): número de cuentas registradas por rol y periodo, porcentaje de acudientes con deportistas vinculados y número de cuentas inactivas. Requiere un reporte nuevo en el contrato (solo `ADMIN`).

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
- **Pendiente de implementar — Documentación de la matrícula** (Definición P1, entradas y actividad "validar la documentación"; SAD 1.4): el acudiente aporta la autorización del acudiente y, si aplica, el certificado médico; el administrador valida la documentación antes de confirmar la matrícula. La decisión del 2026-10-11 de no gestionar soportes documentales queda revocada por la Definición v02. Los IDs `E9–E11`, ausentes del contrato, pueden reservarse para estas operaciones.
- **Pendiente de definir — Renovación por periodo**: la Definición describe la matrícula "para un periodo determinado" y su renovación por periodo. El modelo actual (matrícula continua mes a mes con pago mensual) debe confirmarse como interpretación válida o reemplazarse por una renovación explícita.
- **Pendiente de implementar — Notificación de inscripción exitosa** por correo (salida del P1). Fuera de esta entrega.

### Pagos presenciales
- `E16`: el acudiente reporta que pagó presencialmente; solo activa `paymentReported`, no cambia el estado financiero.
- `E17`: el administrador registra el pago presencial; el monto debe ser al menos la mensualidad vigente y se actualiza a `UP_TO_DATE`. Una matrícula puede tener varios pagos, como máximo uno por mes calendario de `paymentDate`; el pago con `paymentDate` más reciente es el último.
- `E18`: el administrador puede marcar la matrícula `OVERDUE`; `UP_TO_DATE` solo se alcanza registrando un pago. No hay proceso automático de actualización.
- `E19`: consultar historial de pagos con filtrado de pertenencia para acudientes.
- El estado de pago se lleva por matrícula (la mensualidad es por disciplina). La Definición habla del estado de pago "de cada deportista": se debe poder consultar de forma consolidada por deportista.
- No implementar pasarela de pagos en línea.

### Indicadores del Proceso 1
- `R1`: informe de matrículas. Cubre los KPI de la Definición P1: número de inscripciones por periodo, tiempo promedio del proceso de inscripción (`averageConfirmationHours`) y porcentaje de matrículas completas frente a abandonadas (`completionRate`, `abandonmentRate`). **Contratado, pendiente de implementar.**

## Proceso 2 — Gestión de Entrenamientos y Asistencia

### Sesiones y asistencia
- `A1–A6`: crear, listar, generar, consultar, actualizar y cancelar sesiones, con acceso limitado a grupos asignados cuando el actor sea entrenador.
- `A7`: consultar lista de deportistas elegibles para asistencia de una sesión.
- `A8`: registrar la asistencia completa del grupo en una sola llamada; guardar de nuevo sobrescribe marcas anteriores (upsert). La sesión pasa a `COMPLETED`; no se puede registrar asistencia en una sesión futura o cancelada.
- `A9`: consultar historial paginado de asistencia; administradores ven todo, entrenadores sus grupos y acudientes sus deportistas.
- Notificación de horarios y novedades a los acudientes por correo: fuera de esta entrega.

### Indicadores del Proceso 2
- `R2`: informe de asistencia por alcance autorizado.
- `R3`: informe de asistencia de un deportista, con verificación de propiedad.
- `R4`: consulta de ausencias consecutivas, con umbral configurable y filtrado de grupos según rol.

## Fuera de alcance
- Torneos y eventos deportivos, internos o externos: las tablas `tournaments`, `tournament_teams`, `tournament_registrations` y `tournament_matches` del MER se retiran y no deben implementarse.
- Pasarela de pagos, nómina e inventario.

## Contradicciones en los documentos oficiales (pendientes de resolver con los responsables)
- SAD 1.4 incluye "deportista" en la asignación de roles; la Definición establece que el deportista no es usuario. Se aplican los tres roles `ADMIN`, `COACH` y `GUARDIAN`.
- Plan de Calidad: la sección 3.4.3 fija la confiabilidad de notificaciones en ≥ 80 % (solo registro exitoso) y RNF-14 (sección 4.4.5) en ≥ 95 % (registro y recuperación de contraseña). Se toma RNF-14 como requisito vigente.
- El SAD aún menciona torneos en `ReportController`, `ReportService`, la capa de persistencia y el ADR-05 ("eventos y resultados"). Prevalece la exclusión del alcance.
- La tabla de contenido del Plan de Calidad conserva títulos del antiguo Proceso 3 (torneos) para RNF-07 y RNF-11 a RNF-14; prevalece el contenido de la sección 4.

## Pendientes de definición
- Catálogo de eventos de notificación, persistencia en `notifications`, reintentos y relación entre notificación y envío de correo.
- Almacenamiento de los documentos de la matrícula, tipos y tamaño admitidos, y en qué casos aplica el certificado médico.
- Renovación de la matrícula por periodo (ver Proceso 1).
- Tratamiento de consentimiento de datos de menores y sus evidencias con los responsables del proyecto.
