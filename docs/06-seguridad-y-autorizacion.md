# 06. Seguridad y autorización

## Autenticación
El contrato usa JWT Bearer. Los únicos endpoints públicos declarados son `POST /auth/login` y `POST /users/register`. Todos los demás deben requerir token válido salvo que se apruebe una excepción explícita.

El esquema documentado menciona claims `sub`, `role`, `guardianId` o `coachId`, y `exp`. El servidor debe validar firma, expiración, algoritmo permitido y claims necesarios; nunca aceptar roles o identidad enviados libremente por el cliente.

La validación del JWT se implementará con el soporte de resource server de Spring Security (Nimbus), sin librerías JWT de terceros (ver ADR-007).

Códigos de error de autenticación (`401`): `UNAUTHENTICATED` (token ausente o inválido), `TOKEN_EXPIRED` (token vencido) e `INVALID_CREDENTIALS` (login fallido).

## Contraseñas
- Guardar solo hashes adaptativos; el contrato especifica BCrypt.
- No devolver hash en respuestas ni escribir contraseñas en logs.
- Verificar contraseña actual para cambio de contraseña.
- Política de fortaleza (`validation/PasswordPolicy`): de 8 a 72 caracteres, con al menos una mayúscula, una minúscula, un número y un símbolo. Longitud fuera de rango → `400 VALIDATION_ERROR`; composición insuficiente → `422 WEAK_PASSWORD`.
- El login responde el mismo `401 INVALID_CREDENTIALS` para correo inexistente y contraseña incorrecta, y verifica contra un hash ficticio cuando el correo no existe para no revelar cuentas por tiempo de respuesta. `USER_INACTIVE` (403) solo se informa tras validar la contraseña.
- No permitir cambiar rol, correo o documento mediante el endpoint de perfil propio.

## Autorización
La autorización requiere comprobar tanto rol como pertenencia al recurso.

| Actor | Alcance resumido |
|---|---|
| `ADMIN` | Gestión administrativa y consulta global en endpoints que lo permitan. |
| `COACH` | Sus grupos asignados y sesiones/asistencia relacionadas. No acceder a grupos de otros entrenadores. |
| `GUARDIAN` | Sus deportistas, matrículas y pagos. No acceder a datos de otras familias. |

Los listados deben filtrar automáticamente por propiedad en backend; no basta con filtrar en Angular. Para recursos ajenos, el contrato documenta `403 ACCESS_DENIED` en los casos de pertenencia.

## Datos personales de menores
- Minimizar los campos devueltos por cada DTO.
- Evitar exponer información médica, documentos o direcciones salvo necesidad autorizada. Los documentos de la matrícula (autorización del acudiente y certificado médico) solo pueden consultarlos el acudiente dueño y el administrador.
- Validar y conservar la aceptación de la política de tratamiento de datos según requisitos.
- No enviar a Brevo más información que la necesaria para el correo. Implementado: a Brevo solo van el correo y el nombre del destinatario, el asunto y el cuerpo; los datos variables escritos por usuarios (nombres, motivos) se escapan en el HTML. `notifications` guarda tipo, asunto, estado e intentos, sin la dirección ni el cuerpo. Los logs de envío solo incluyen el id de la notificación, el tipo y el código de error del proveedor. Las cuentas inactivas no reciben correos.
- La clave de Brevo (`BREVO_API_KEY`) solo vive en variables de entorno; `BrevoProperties.toString()` y `EmailMessage.toString()` no muestran la clave ni las direcciones.
- No usar datos reales de menores en pruebas compartidas o ejemplos.
- La revisión técnica no reemplaza la validación legal de cumplimiento de la Ley 1581 de 2012.
- Decisiones del 2026-11-09: la autorización del acudiente en U2 cubre a todos sus deportistas; la supresión se atenderá inactivando el perfil, como mejora futura; el entrenador ve la edad y el contacto del acudiente solo de los deportistas de sus grupos; la auditoría de consultas a datos de menores queda como posible implementación futura.

## Errores y respuestas
- `400`: formato o validación de campos.
- `401`: falta autenticación o credenciales/token inválidos.
- `403`: rol o alcance insuficiente.
- `404`: recurso inexistente.
- `409`: duplicado o conflicto de estado.
- `422`: regla de negocio incumplida.
- La estructura `Error` debe mantener `code` estable y `message` en español. El frontend nunca debe depender del texto de `message`.

## Pruebas obligatorias
- Token ausente, inválido y expirado.
- Token vigente de una cuenta inactivada.
- Recuperación de contraseña: token vencido, reutilizado o alterado, y respuesta idéntica para correos existentes e inexistentes.
- Usuario autenticado con rol incorrecto.
- Usuario con rol correcto intentando acceder a recurso ajeno.
- Acudiente manipulando `guardianId`/`athleteId` en el request.
- Entrenador accediendo a grupo no asignado.
- Cambio de rol o campos protegidos por asignación masiva.
- No exposición de hash, token o información sensible en respuesta/log.

## Implementación (HU28)
- `security/SecurityConfig`: sin sesión; públicos solo `POST /auth/login`, `POST /users/register`, `/actuator/health` y la documentación OpenAPI; todo lo demás exige JWT.
- CSRF desactivado: la credencial viaja en la cabecera `Authorization` (no en cookies), que el navegador no adjunta automáticamente a solicitudes de otros sitios.
- `security/JwtConfig`: solo HS256; valida firma, `exp` (sin tolerancia de reloj), `sub` presente y `role` dentro de `ADMIN`, `COACH`, `GUARDIAN`.
- `security/SecurityErrorHandler`: `401 TOKEN_EXPIRED` para token vencido, `401 UNAUTHENTICATED` para token ausente o inválido, `403 ACCESS_DENIED` por rol; mismo formato `Error`.
- El token no contiene datos personales: solo `sub`, `role`, `guardianId` o `coachId`, `iat` y `exp`. `sub` debe ser un id numérico.
- `security/CurrentUserService` (HU29) es la única fuente de identidad de las operaciones propias: lee el JWT validado y nunca usa ids, roles o correos enviados por el cliente.
- Autorización por rol (HU30): las rutas exclusivas de `ADMIN` (`/users` salvo `/users/me` y el registro, `/coaches`, `/guardians`) se restringen en `SecurityConfig`, de modo que un usuario sin permiso recibe `403` antes de que se lea o valide el cuerpo. Los controladores repiten la regla con `@PreAuthorize` como segunda barrera, y `GlobalExceptionHandler` traduce ese rechazo a `403 ACCESS_DENIED`.
- Disciplinas (HU32): `GET /disciplines/**` para cualquier rol autenticado; `POST`, `PUT` y `PATCH` solo `ADMIN`, restringidos en el filtro y con `@PreAuthorize`.
- Deportistas (HU34): `/athletes/**` solo `ADMIN` y `GUARDIAN` (filtro y `@PreAuthorize`). La propiedad se comprueba en `service/AthleteAccessPolicy` con el `guardianId` del token: un acudiente nunca ve ni modifica deportistas ajenos, y el `guardianId` que envíe se ignora. Las respuestas solo incluyen los campos del contrato y los logs no registran datos de los menores.
- Matrículas (HU35): `/availability` y `/enrollments/**` solo `ADMIN` y `GUARDIAN` (filtro y `@PreAuthorize`). La propiedad se comprueba con `service/AthleteAccessPolicy` sobre el deportista de la matrícula; las respuestas solo exponen id, nombres y apellidos del deportista. Desde HU37, `/enrollments/*/confirm`, `/enrollments/*/reject` y `/enrollments/*/group` son solo `ADMIN`; `/enrollments/*/cancel` admite al acudiente dueño, que no puede cancelar una matrícula `ACTIVE`.
- Pagos (HU36): `/enrollments/*/payments` y `/enrollments/*/payment-status` solo `ADMIN`; `/enrollments/*/payment-report` solo `GUARDIAN`, sobre matrículas de sus deportistas; `/payments` `ADMIN` y `GUARDIAN`, y el acudiente solo recibe los pagos de sus deportistas (el filtro lo fija el token). Estas reglas se evalúan antes que la regla general de `/enrollments/**`.
- Sesiones y asistencia (HU39): `GET /sessions/*/attendance` para `ADMIN` y `COACH`, y su `PUT` solo `COACH`; `GET /sessions/**` para los tres roles; el resto de `/sessions/**` para `ADMIN` y `COACH`; `/attendance-records` para los tres roles (filtro y `@PreAuthorize`). En el servicio, `GroupAccessPolicy` limita al entrenador a los grupos que tiene a cargo (y solo él registra la asistencia), y el acudiente solo ve sesiones de grupos donde sus deportistas tienen una matrícula `ACTIVE` ya iniciada y solo las marcas de sus deportistas. Las respuestas exponen del deportista solo id, nombres y apellidos.
- Reportes (Entrega 3, etapas 3 y 4): `/reports/enrollments` (R1) y `/reports/users` (R5) solo `ADMIN`; `/reports/attendance/athletes/*` (R3) los tres roles; el resto de `/reports/**` (R2, R4) para `ADMIN` y `COACH` (filtro y `@PreAuthorize`). En el servicio, el entrenador solo agrega datos de sus grupos y, en R3, solo consulta a deportistas con matrícula vigente en uno de ellos (`403` si no); el acudiente solo consulta a sus deportistas con `AthleteAccessPolicy`. Los reportes exponen del deportista solo id, nombres y apellidos; R5 solo devuelve conteos, sin datos personales.
- Lista de asistencia (A7, A8): incluye la edad del deportista y el contacto del acudiente (nombres, apellidos y teléfono), y nada más de su cuenta (ni correo ni documento). Solo la ven el administrador y el entrenador actual del grupo (`GroupAccessPolicy`); otro entrenador o un acudiente reciben `403`.
- Grupos y franjas (HU33): `GET /groups/**` para `ADMIN` y `COACH`; escritura en `/groups/**` y `/schedules/**` solo `ADMIN` (filtro y `@PreAuthorize`). La propiedad de los grupos se comprueba en `service/GroupAccessPolicy` con el `coachId` del token.

## Pendientes de seguridad (Proceso 3, documentos oficiales v02)
- **Recuperación de contraseña** (ADR-07: "mecanismo seguro de recuperación de contraseña"): token aleatorio de un solo uso, guardado solo como hash, con vencimiento corto; la solicitud responde igual exista o no el correo; el restablecimiento aplica `PasswordPolicy`; los endpoints son públicos y deben añadirse explícitamente en `SecurityConfig`. Se construye después de la integración de correo (decisión del 2026-11-09). **Implementado el 2026-11-10 (U13, U14):** token de 256 bits de `SecureRandom` en Base64 URL, guardado como SHA-256; vigencia de 20 minutos (`app.password-reset.token-validity`; ajustada de 30 a 20 el 2026-11-11); una solicitud nueva o un restablecimiento invalidan los demás tokens abiertos de la cuenta; el restablecimiento bloquea la fila del token (`PESSIMISTIC_WRITE`), así que dos usos simultáneos solo cambian la contraseña una vez; un token inválido, vencido, usado o de una cuenta inactiva responde el mismo `422 INVALID_RESET_TOKEN`, y el token nunca aparece en logs, respuestas, `toString()` ni en `notifications`. Los JWT ya emitidos no se revocan. Limitación: no hay límite de solicitudes por correo o IP; repetir la solicitud solo reemplaza el enlace, pero puede usarse para enviar correos repetidos a una cuenta.
- **Cuentas inactivas con token vigente** (posible mejora futura (decisión del 2026-11-09); no es prioridad): hoy el JWT no se contrasta con `users.status`, de modo que una cuenta inactivada (U10) puede operar hasta que su token venza. El Plan de Calidad exige rechazar los accesos de cuentas inactivas; se debe verificar el estado del usuario al autenticar cada solicitud y responder `403 USER_INACTIVE` (o el código que fije el contrato).
- ~~**Duplicados concurrentes**~~ **Resuelto (2026-11-09, Entrega 3, etapa 2):** si dos solicitudes simultáneas con el mismo correo o documento superan la comprobación previa, la restricción única de PostgreSQL rechaza la segunda. `AccountCreator` envía la escritura a la base dentro del caso de uso y traduce ese rechazo al `409` correspondiente (U2, U8 y el cambio de correo de U9). `AccountConflictLookup` identifica el dato duplicado en una transacción nueva, sin depender de los nombres de las restricciones de la base desplegada; la respuesta no expone SQL ni nombres de restricciones (RNF-11, RNF-13).
- **Cierre de sesión**: se resuelve en el cliente descartando el token; cuando se implemente la verificación de cuentas inactivas no se requerirá lista de revocación.

## CORS y secretos
Configurar orígenes permitidos de forma explícita en la configuración general; no usar comodines con credenciales. Guardar claves JWT, credenciales de base de datos y claves de Brevo en variables de entorno, nunca en el repositorio.
