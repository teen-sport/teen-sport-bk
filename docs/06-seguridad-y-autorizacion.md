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
- Aplicar política de fortaleza conforme al contrato y devolver `WEAK_PASSWORD` cuando corresponda.
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

## CORS y secretos
Configurar orígenes permitidos de forma explícita en la configuración general; no usar comodines con credenciales. Guardar claves JWT, credenciales de base de datos y claves de Brevo en variables de entorno, nunca en el repositorio.
