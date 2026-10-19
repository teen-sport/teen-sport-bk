# CLAUDE.md — TeenSport Backend

## 1. Contexto del proyecto

TeenSport es una API REST desarrollada para digitalizar la operación de una academia deportiva multidisciplinar.

El sistema se organiza en tres procesos de negocio:

- Proceso 1 — Gestión de Inscripción y Matrícula: perfiles de deportistas seleccionados por el acudiente, disciplinas, grupos, horarios y cupos, documentación de la matrícula, confirmación, pagos presenciales y estado de pago (al día / en mora).
- Proceso 2 — Gestión de Entrenamientos y Asistencia: sesiones, asignación de entrenadores, registro y consulta de asistencia e indicadores.
- Proceso 3 — Registro, autenticación y gestión de usuarios: autorregistro de acudientes, creación de entrenadores y administradores, inicio y cierre de sesión, recuperación de contraseña por correo, perfiles, activación e inactivación de cuentas y vinculación de deportistas a su acudiente.

Además genera reportes e indicadores de los procesos y notificaciones por correo. Los torneos y eventos, internos o externos, están fuera del alcance.

El frontend Angular y el backend son aplicaciones independientes. Este repositorio contiene exclusivamente el backend.

## 2. Stack tecnológico

- Java.
- Spring Boot.
- Spring Security.
- Spring Data JPA e Hibernate.
- Base de datos relacional definida por la arquitectura y el MER aprobado.
- JWT para autenticación y autorización.
- Bean Validation para validar entradas.
- JUnit y Mockito para pruebas.
- JaCoCo para medir cobertura.
- OpenAPI/Swagger para documentar la API.
- Brevo para notificaciones por correo, mediante un adaptador desacoplado.
- Git para control de versiones.

Antes de agregar dependencias o cambiar versiones, revisa la configuración real del proyecto. No inventes versiones ni sustituyas tecnologías sin autorización.

## 3. Fuentes de verdad

Los documentos oficiales del proyecto prevalecen sobre la documentación de este repositorio:

- Definición del Proyecto (versión 02, 19/10/2026): alcance, procesos de negocio, actores y KPI.
- Plan de Gestión de la Calidad del Proyecto (versión 02, 19/10/2026): métricas y requisitos no funcionales RNF-01 a RNF-17.
- Software Architecture Description – TeenSport (C4 y ADR-01 a ADR-10).

La documentación de `docs/` los traduce al backend. Si encuentras una diferencia entre ellos, los documentos oficiales mandan: registra la diferencia y adapta el proyecto.

Antes de implementar una funcionalidad, consulta los documentos pertinentes:

- `docs/01-contexto-del-proyecto.md`
- `docs/02-requisitos-funcionales.md`
- `docs/03-requisitos-no-funcionales.md`
- `docs/04-modelo-entidad-relacion.md`
- `docs/05-arquitectura-backend.md`
- `docs/06-seguridad-y-autorizacion.md`
- `docs/07-contrato-api.md`
- `docs/08-estrategia-de-pruebas.md`
- `docs/09-trazabilidad-requisitos.md`
- `docs/10-plan-implementacion.md`

Si un documento está pendiente, no supongas que su contenido está definido.

En caso de contradicción, identifica el conflicto, explica sus implicaciones y solicita una decisión antes de implementar cambios que afecten el modelo de datos, la arquitectura o las reglas de negocio.

## 4. Principios de desarrollo obligatorios

- Aplicar separación de responsabilidades y arquitectura por capas.
- Mantener funciones, métodos y clases con una responsabilidad claramente definida.
- Seguir SOLID, DRY y KISS cuando sean pertinentes, evitando abstracciones innecesarias.
- Priorizar código legible, cohesivo, mantenible y comprobable.
- Evitar duplicación de lógica de negocio.
- No introducir patrones, librerías o capas sin una necesidad concreta.
- No crear métodos vacíos, implementaciones simuladas ni funcionalidades ficticias.
- No implementar funcionalidades fuera del alcance aprobado.
- No modificar contratos públicos o esquemas de datos sin justificarlo y documentarlo.

## 5. Arquitectura por capas

Organizar el backend en las capas definidas en `docs/05-arquitectura-backend.md`.

Responsabilidades generales:

- Controller: recibe solicitudes HTTP, valida la entrada y devuelve respuestas.
- Service: implementa y coordina las reglas de negocio y los casos de uso.
- Repository: encapsula el acceso a datos mediante Spring Data JPA o consultas justificadas.
- Entity: representa el modelo persistente según el MER aprobado.
- DTO: define los contratos de entrada y salida de la API.
- Mapper: transforma DTO y entidades cuando se requiera.
- Config: centraliza configuración transversal.
- Exception: define excepciones de negocio y el tratamiento uniforme de errores.
- Integration: encapsula las dependencias externas, como Brevo.

No colocar reglas de negocio en los controllers ni consultas de persistencia directamente en ellos.

Los servicios deben coordinar las operaciones transaccionales y los repositorios deben concentrarse en el acceso a datos.

## 6. Calidad del código

- Utilizar nombres claros y consistentes en inglés para clases, métodos y variables.
- Mantener los mensajes de la API en español.
- Aplicar validación explícita de entradas mediante Bean Validation y reglas de negocio.
- Evitar métodos excesivamente largos y anidamientos innecesarios.
- No utilizar excepciones genéricas como mecanismo normal de control de flujo.
- Evitar capturar excepciones para ignorarlas o esconder sus causas.
- No devolver entidades JPA directamente desde los endpoints.
- No exponer contraseñas, hashes, tokens, secretos ni datos personales innecesarios.
- Documentar las decisiones no evidentes y las reglas de negocio complejas, no cada línea de código.

## 7. Seguridad obligatoria

- Aplicar Spring Security para proteger los endpoints.
- Autenticar mediante JWT y verificar firma, vigencia y claims relevantes.
- Definir expiración configurable de los tokens.
- Aplicar autorización por rol en el backend; no confiar en las restricciones visuales del frontend.
- Verificar la propiedad o el alcance de los recursos consultados por cada usuario.
- Almacenar contraseñas con un algoritmo de hash seguro para contraseñas, mediante las abstracciones de Spring Security.
- No almacenar contraseñas en texto plano ni implementar algoritmos criptográficos propios.
- Validar, normalizar cuando corresponda y limitar los datos de entrada.
- Usar consultas parametrizadas y mecanismos seguros de persistencia.
- Mantener secretos y credenciales fuera del repositorio.
- Respetar el principio de mínimo privilegio.
- Proteger especialmente los datos personales de deportistas menores de edad.
- Probar accesos permitidos y denegados para cada rol.

## 8. Manejo de errores

- Centralizar el tratamiento de errores HTTP mediante `@RestControllerAdvice`.
- Diferenciar errores de validación, autenticación, autorización, recursos inexistentes, conflictos de negocio y fallos inesperados.
- Utilizar códigos HTTP coherentes y un formato de error consistente.
- No exponer stack traces, consultas SQL, detalles internos ni secretos al cliente.
- Registrar información técnica útil en logs sin incluir credenciales ni datos personales innecesarios.
- No convertir silenciosamente errores inesperados en respuestas exitosas.

## 9. Persistencia e integridad

- Tomar `docs/04-modelo-entidad-relacion.md` como referencia para el modelo de datos.
- Mantener las relaciones, cardinalidades y restricciones definidas en el MER aprobado.
- Utilizar transacciones para operaciones que deban ejecutarse de forma atómica.
- Aplicar restricciones de integridad en la base de datos cuando corresponda.
- Usar JPA como mecanismo principal de persistencia.
- Introducir SQL nativo únicamente cuando exista una justificación de rendimiento o complejidad.
- Evitar consultas N+1, cargas innecesarias y exposición accidental de relaciones.
- No utilizar datos de prueba en producción.
- No ejecutar cambios destructivos sobre la base de datos sin autorización explícita.

## 10. Pruebas y calidad

Cada funcionalidad debe incluir las pruebas pertinentes:

- Pruebas unitarias de las reglas de negocio.
- Pruebas de validaciones y casos límite.
- Pruebas de persistencia e integridad.
- Pruebas de autorización por rol y acceso a recursos.
- Pruebas de errores esperados y no esperados.
- Pruebas de integración cuando exista interacción entre componentes.

Utilizar JUnit y Mockito, y JaCoCo para medir la cobertura.

No declarar una funcionalidad terminada si no se han ejecutado las pruebas correspondientes o si existen fallos conocidos sin documentar.

Respetar los criterios de aceptación de `docs/03-requisitos-no-funcionales.md`.

## 11. Integración de correo

La lógica de negocio no debe depender directamente del SDK de Brevo.

Utilizar un componente de notificaciones y un adaptador de integración que permita sustituir al proveedor sin reescribir los servicios de negocio.

Gestionar fallos de envío sin afirmar que una notificación fue enviada cuando no existe evidencia de ello.

Mantener las credenciales del proveedor en configuración segura.

## 12. Flujo de trabajo con Claude Code

Antes de modificar código:

1. Analiza el requisito y sus criterios de aceptación.
2. Consulta el MER, la arquitectura y las reglas de seguridad pertinentes.
3. Inspecciona el código existente y sus convenciones.
4. Identifica las clases y capas que deben modificarse.
5. Propón un plan breve y los riesgos relevantes.

Durante la implementación:

1. Trabaja en cambios pequeños y enfocados.
2. Implementa solo el alcance solicitado.
3. Añade o actualiza las pruebas.
4. Actualiza la documentación y la trazabilidad afectadas.
5. No modifiques archivos no relacionados.

Al terminar:

1. Ejecuta las pruebas pertinentes y la compilación.
2. Informa los comandos ejecutados y sus resultados reales.
3. Identifica los requisitos cubiertos.
4. Indica decisiones pendientes, limitaciones y fallos conocidos.
5. Actualiza el estado de la tarea en `docs/10-plan-implementacion.md`.

No afirmes que una prueba, compilación o despliegue fue exitoso si no se ejecutó y verificó.

## 13. Límites del alcance

No implementar:

- Pasarelas de pago en línea.
- Nómina ni contratación.
- Inventario de implementos deportivos.
- Organización de eventos o torneos deportivos, internos o con participantes externos a la academia.

Los pagos son presenciales y su estado se actualiza manualmente según las responsabilidades definidas en los requisitos.

## 14. Criterio de finalización

Una tarea solo puede considerarse terminada cuando:

- Cumple el requisito aprobado.
- Respeta las capas y responsabilidades.
- Incluye validaciones y controles de seguridad pertinentes.
- Maneja los errores esperados.
- Tiene pruebas adecuadas que pasan.
- Mantiene la documentación y los contratos actualizados.
- No introduce regresiones conocidas.

Si falta información crítica, explica qué falta y solicita una decisión en lugar de inventar la solución.