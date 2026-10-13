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
- Evitar exponer información médica, documentos o direcciones salvo necesidad autorizada.
- Validar y conservar la aceptación de la política de tratamiento de datos según requisitos.
- No enviar a Brevo más información que la necesaria para el correo.
- No usar datos reales de menores en pruebas compartidas o ejemplos.
- La revisión técnica no reemplaza la validación legal de cumplimiento de la Ley 1581 de 2012.

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

## CORS y secretos
Configurar orígenes permitidos de forma explícita en la configuración general; no usar comodines con credenciales. Guardar claves JWT, credenciales de base de datos y claves de Brevo en variables de entorno, nunca en el repositorio.
