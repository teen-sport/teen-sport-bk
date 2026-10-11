# Reglas de seguridad

## Autenticación

- Implementar autenticación mediante Spring Security y JWT.
- Verificar la firma, expiración y claims relevantes del token.
- Rechazar tokens expirados, malformados o con firma inválida.
- No aceptar la identidad o el rol enviado libremente en el cuerpo de una petición como prueba de autorización.
- Mantener las credenciales criptográficas en variables de entorno o en un gestor de secretos.

## Contraseñas

- Utilizar un algoritmo de hash adaptativo seguro para contraseñas, mediante `PasswordEncoder`.
- No almacenar contraseñas en texto plano.
- No implementar algoritmos criptográficos propios.
- No registrar contraseñas ni hashes en logs.
- No permitir que las respuestas de la API expongan el campo de contraseña.

## Autorización

- Aplicar mínimo privilegio.
- Definir permisos de acuerdo con los roles aprobados.
- Proteger los endpoints en el backend.
- Verificar también si el usuario puede acceder al recurso concreto solicitado.
- Un acudiente solo debe consultar y administrar los recursos de los deportistas que tiene autorizados.
- Un entrenador solo debe acceder a los grupos, sesiones y asistencias que le corresponden.
- Las operaciones administrativas deben estar reservadas al rol autorizado.
- Probar expresamente accesos denegados.

## Datos personales

- Minimizar la información personal expuesta en respuestas y registros.
- Validar permisos antes de devolver datos de menores.
- Evitar filtraciones de información entre acudientes, entrenadores y administradores.
- Considerar las obligaciones aplicables de protección de datos personales en Colombia.

## API

- Validar todas las entradas.
- Usar consultas parametrizadas y mecanismos seguros de persistencia.
- Configurar CORS con orígenes explícitamente permitidos.
- No habilitar CORS indiscriminadamente.
- No deshabilitar CSRF sin analizar el mecanismo de autenticación y el almacenamiento de credenciales.
- No exponer detalles internos en mensajes de error.
- Mantener Swagger protegido o limitado según el entorno y la política del proyecto.

## Pruebas mínimas

- Credenciales correctas e incorrectas.
- Token ausente, inválido y expirado.
- Acceso permitido y denegado por rol.
- Acceso a recursos pertenecientes a otro usuario.
- Intentos de asignar privilegios no autorizados.
- Verificación de que las respuestas no contienen contraseñas ni datos internos sensibles.