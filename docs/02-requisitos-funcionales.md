# 02. Requisitos funcionales

> Esta lista resume el comportamiento que se desprende del OpenAPI entregado. Los identificadores `x-endpoint-id` se conservan como trazabilidad del contrato; no sustituyen los IDs de requisitos académicos si existe una matriz oficial aparte.

## Autenticación y usuarios
- `U1`: autenticar por correo/credenciales y devolver JWT; error genérico para correo o contraseña incorrectos.
- `U2`: registrar una cuenta de acudiente, con aceptación obligatoria de la política de tratamiento de datos; crear usuario y acudiente en una transacción.
- `U3–U5`: consultar/actualizar perfil propio y cambiar contraseña; no permitir cambiar correo, documento o rol mediante la actualización del perfil.
- `U6–U12`: administración de usuarios, consulta individual, creación de personal, cambio de estado, listado de entrenadores y acudientes.
- Los roles `COACH` y `ADMIN` los crea un administrador; el registro público está limitado a acudientes.

## Deportistas
- `E1–E4`: registrar, listar, consultar y actualizar deportistas.
- La edad debe estar entre 12 y 17 años en la fecha de la solicitud, según contrato.
- Los acudientes solo pueden gestionar deportistas asociados a su cuenta; el administrador puede operar en nombre de un acudiente con las reglas documentadas.
- No existe DELETE de deportistas en el contrato; la salida de la academia se representa mediante la gestión de la matrícula.

## Disponibilidad y matrículas
- `E5`: consultar grupos activos de disciplinas activas y disponibilidad para un periodo.
- El `period` de una matrícula es su mes de inicio; la matrícula sigue vigente mes a mes hasta ser rechazada o cancelada.
- Los cupos ocupados de un mes cuentan las matrículas `PENDING_REVIEW` y `ACTIVE` con mes de inicio igual o anterior.
- `E6`: solicitar matrícula; estado inicial `PENDING_REVIEW`, pago inicial `OVERDUE` y reserva de cupo bajo bloqueo pesimista del grupo.
- Un deportista no puede tener dos matrículas vigentes (`PENDING_REVIEW` o `ACTIVE`) en la misma disciplina, sin importar grupo ni mes de inicio (`DUPLICATE_ENROLLMENT`); sí en disciplinas distintas.
- Si no hay cupos, no se crea matrícula, se intenta notificar por correo y se devuelve conflicto.
- `E7–E8`: listar matrículas y consultar detalle con horarios, aplicando permisos de propiedad.
- `E12`: administrador confirma matrícula, puede reasignar grupo de la misma disciplina y registrar un pago en la misma operación. Si no registra pago, la matrícula queda `OVERDUE`.
- `E13`: administrador rechaza matrícula pendiente y libera cupo.
- `E14`: cancelar matrícula conforme al rol y estado; liberar cupo cuando corresponda.
- `E15`: cambiar de grupo dentro de la misma disciplina, conservando el mes de inicio; liberar el cupo anterior y reservar el nuevo sin modificar sesiones pasadas ni asistencia.

## Pagos presenciales
- `E16`: el acudiente reporta que pagó presencialmente; solo activa `paymentReported`, no cambia el estado financiero.
- `E17`: el administrador registra el pago presencial; el monto debe ser al menos la mensualidad vigente y se actualiza a `UP_TO_DATE`. Una matrícula puede tener varios pagos, como máximo uno por mes calendario de `paymentDate`; el pago con `paymentDate` más reciente es el último.
- `E18`: el administrador puede marcar la matrícula `OVERDUE`; `UP_TO_DATE` solo se alcanza registrando un pago. No hay proceso automático de actualización.
- `E19`: consultar historial de pagos con filtrado de pertenencia para acudientes.
- No implementar pasarela de pagos en línea.

## Disciplinas, grupos y horarios
- `H1–H5`: listar, consultar, crear, actualizar y activar/desactivar disciplinas. La mensualidad debe ser positiva. No se puede desactivar una disciplina con grupos activos.
- `H6–H11`: listar, consultar, crear, actualizar, activar/desactivar grupos y asignar entrenador.
- La capacidad de grupo está entre 1 y 15. El entrenador y la disciplina deben estar activos al crear el grupo. El grupo se almacena como `training_groups` por ser `groups` palabra reservada en MySQL 8.
- No reducir la capacidad por debajo de las matrículas vigentes (`PENDING_REVIEW` y `ACTIVE`) del grupo.
- `H12–H14`: crear, actualizar y eliminar horarios del grupo conforme a las reglas del contrato.

## Sesiones y asistencia
- `A1–A6`: crear, listar, generar, consultar, actualizar y cancelar sesiones, con acceso limitado a grupos asignados cuando el actor sea entrenador.
- `A7`: consultar lista de deportistas elegibles para asistencia de una sesión.
- `A8`: registrar la asistencia completa del grupo en una sola llamada; guardar de nuevo sobrescribe marcas anteriores (upsert). La sesión pasa a `COMPLETED`; no se puede registrar asistencia en una sesión futura o cancelada.
- `A9`: consultar historial paginado de asistencia; administradores ven todo, entrenadores sus grupos y acudientes sus deportistas.

## Informes
- `R1`: informe de matrículas.
- `R2`: informe de asistencia por alcance autorizado.
- `R3`: informe de asistencia de un deportista, con verificación de propiedad.
- `R4`: consulta de ausencias consecutivas, con umbral configurable y filtrado de grupos según rol.

## Pendientes de definición
- El MER incluye torneos, equipos, inscripciones y partidos, pero el OpenAPI no incluye endpoints de torneos. Definir operaciones y reglas antes de implementarlas.
- El MER incluye notificaciones, pero se debe confirmar el catálogo de eventos, persistencia, reintentos y relación entre notificación y envío de correo.
- Confirmar el tratamiento de consentimiento de datos de menores y sus evidencias con los responsables del proyecto.
