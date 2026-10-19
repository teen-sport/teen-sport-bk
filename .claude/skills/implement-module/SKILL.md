---
name: implement-module
description: Analiza, planifica, implementa y prueba funcionalidades del backend de TeenSport siguiendo los requisitos del proyecto, la arquitectura por capas y las reglas de calidad. Utilizar al crear, modificar o completar módulos, endpoints, servicios, repositorios o funcionalidades del backend.
---

# Implementación de módulos del backend de TeenSport

## 1. Objetivo

Implementar funcionalidades del backend de TeenSport de forma incremental, mantenible, segura y verificable.

Antes de modificar código, debes comprender el requerimiento, identificar las capas afectadas, revisar las convenciones existentes y definir cómo se validará el resultado.

No debes generar módulos completos ni modificar múltiples áreas sin justificar por qué es necesario.

## 2. Contexto del proyecto

TeenSport es un sistema de información para una academia deportiva multidisciplinaria. Busca centralizar procesos que actualmente pueden realizarse mediante registros manuales, seguimiento de pagos, coordinación por WhatsApp y control de asistencia en papel.

El backend debe exponer una API REST desarrollada con Java y Spring Boot. El frontend Angular es independiente del backend.

Los roles funcionales del sistema son:

- ADMINISTRADOR: crea y administra las cuentas de entrenadores y administradores, activa o inactiva cuentas, gestiona disciplinas, grupos, cupos y horarios, valida la documentación y confirma matrículas, actualiza estados de pago y consulta informes.
- ENTRENADOR: consulta y gestiona las sesiones de entrenamiento que le corresponden, registra asistencias de sus deportistas y consulta informes de asistencia de sus grupos.
- ACUDIENTE: se autorregistra, gestiona los perfiles y las inscripciones de sus deportistas, aporta la documentación de la matrícula, consulta estados de pago y asistencia, y puede reportar pagos realizados presencialmente.

El deportista no es usuario del sistema; su perfil lo administra su acudiente.

Los deportistas son menores de edad, entre 12 y 17 años, según el alcance documentado. Los datos deben tratarse con especial cuidado.

Antes de implementar una funcionalidad, consulta los documentos del proyecto y verifica sus reglas específicas. No inventes reglas de negocio que no estén documentadas.

## 3. Fuentes de verdad

Antes de comenzar, consulta cuando estén disponibles:

1. `CLAUDE.md`: reglas generales del repositorio.
2. `.claude/rules/`: reglas de arquitectura, Java, seguridad, pruebas, errores, documentación y base de datos.
3. `docs/01-contexto-del-proyecto.md`: contexto y alcance.
4. `docs/02-requisitos-funcionales.md`: requisitos funcionales.
5. `docs/03-requisitos-no-funcionales.md`: requisitos de calidad.
6. `docs/04-modelo-entidad-relacion.md`: modelo de datos aprobado.
7. `docs/05-arquitectura-backend.md`: estructura técnica.
8. `docs/06-seguridad-y-autorizacion.md`: autenticación y autorización.
9. `docs/07-contrato-api.md`: convenciones y contratos REST.
10. `docs/08-estrategia-de-pruebas.md`: estrategia de pruebas.
11. `docs/09-trazabilidad-requisitos.md`: relación entre requisitos y pruebas.
12. Los ADR disponibles en `docs/adr/`.

Los nombres anteriores corresponden a la estructura documental propuesta. Si algún archivo todavía no existe, no asumas que su contenido ya está definido: consulta las fuentes disponibles y señala la información faltante.

Si dos fuentes se contradicen, no decidas silenciosamente cuál prevalece. Expón la contradicción y solicita una decisión cuando afecte el diseño o el comportamiento funcional.

## 4. Flujo obligatorio de trabajo

### Paso 1. Comprender el requerimiento

- Identifica el requisito funcional que se implementará.
- Resume el comportamiento esperado.
- Identifica el actor y los roles autorizados.
- Define los datos de entrada, las validaciones y el resultado esperado.
- Identifica las reglas de negocio, las restricciones y los posibles errores.
- Determina si existen dependencias con otros módulos.
- Identifica los requisitos no funcionales relacionados.

Si falta una regla de negocio indispensable, pregunta antes de implementarla.

### Paso 2. Inspeccionar el código existente

Antes de crear o modificar archivos:

- Revisa la estructura real del proyecto.
- Comprueba las versiones de Java, Spring Boot y las dependencias declaradas.
- Inspecciona las clases relacionadas con la funcionalidad.
- Reutiliza componentes existentes cuando corresponda.
- Identifica convenciones de nombres, DTO, excepciones y respuestas HTTP.
- Verifica las entidades JPA relacionadas con los datos y su correspondencia con el MER.

No cambies versiones, dependencias, configuraciones globales ni tecnologías sin una justificación concreta.

### Paso 3. Elaborar un plan de implementación

Presenta un plan breve que indique:

- Archivos que se crearán o modificarán.
- Responsabilidad de cada archivo.
- Cambios en las capas involucradas.
- Reglas de validación.
- Permisos y restricciones de acceso.
- Pruebas unitarias y de integración necesarias.
- Requisitos documentales que deben actualizarse.

Evita planes desproporcionados para funcionalidades pequeñas.

### Paso 4. Implementar por capas

Respeta la arquitectura por capas definida para TeenSport.

- **Controller:** recibe solicitudes HTTP, aplica las validaciones de entrada correspondientes y delega el caso de uso al servicio.
- **Service:** coordina el caso de uso y aplica las reglas de negocio.
- **Repository:** gestiona el acceso a datos y la persistencia.
- **Entity:** representa el modelo persistente según el diseño aprobado.
- **DTO:** define los datos que entran y salen de la API.
- **Mapper:** transforma entre DTO y entidades cuando esta separación sea necesaria.
- **Config y Security:** centralizan la configuración técnica y de seguridad.
- **Exception:** centraliza el tratamiento de errores de la aplicación.
- **Integration:** encapsula integraciones externas, como el envío de correos mediante el adaptador de Brevo.

No introduzcas arquitectura de microservicios ni arquitectura hexagonal sin una decisión explícita aprobada para el proyecto.

No agregues interfaces, abstracciones o clases auxiliares que no aporten una responsabilidad clara.

### Paso 5. Aplicar buenas prácticas de implementación

- Cada método debe tener una responsabilidad clara.
- Evita métodos excesivamente largos y clases con demasiadas responsabilidades.
- Usa nombres descriptivos y coherentes con el dominio.
- Evita duplicar reglas de negocio.
- Mantén la lógica de negocio fuera de los controladores y repositorios.
- Usa inyección de dependencias mediante los mecanismos de Spring.
- Evita estados mutables innecesarios y dependencias circulares.
- Aplica principios SOLID cuando mejoren la claridad y mantenibilidad.
- No utilices comentarios para justificar código confuso: simplifica el código.
- No implementes funcionalidades fuera del alcance aprobado.

### Paso 6. Validar seguridad y permisos

Para cada endpoint, determina:

- Si requiere autenticación.
- Qué roles pueden utilizarlo.
- Qué recurso puede consultar o modificar cada actor.
- Si el usuario puede acceder únicamente a los registros que le corresponden.
- Qué datos deben validarse antes de ejecutar la operación.

La autenticación por JWT no reemplaza la autorización sobre los recursos.

No confíes en el ID, el rol ni la identidad que el cliente envíe libremente en el cuerpo de la solicitud. La identidad autenticada debe obtenerse del contexto de seguridad validado por el servidor cuando corresponda.

### Paso 7. Implementar manejo de errores

- Valida entradas inválidas.
- Define excepciones de dominio cuando sean necesarias.
- Centraliza el manejo de errores HTTP.
- Devuelve códigos HTTP coherentes.
- No expongas trazas, contraseñas, tokens, consultas SQL ni detalles internos.
- No ocultes excepciones con bloques catch vacíos.
- No devuelvas respuestas exitosas si la operación no se completó correctamente.
- Protege las operaciones que necesiten consistencia transaccional.

### Paso 8. Crear pruebas

Incluye las pruebas apropiadas para el comportamiento implementado:

- Pruebas unitarias de servicios y reglas de negocio.
- Pruebas de validación.
- Pruebas de controladores y respuestas HTTP.
- Pruebas de integración con persistencia cuando corresponda.
- Pruebas de autorización por rol y de acceso a recursos ajenos.
- Pruebas de escenarios exitosos y fallidos.

Las pruebas deben verificar resultados y comportamientos relevantes, no limitarse a ejecutar líneas de código.

### Paso 9. Documentar

- Documenta clases y métodos públicos cuando corresponda mediante Javadoc.
- Explica las reglas de negocio que no sean evidentes.
- Actualiza el contrato API cuando cambien endpoints, parámetros o respuestas.
- Actualiza la trazabilidad de requisitos cuando corresponda.
- Documenta las decisiones arquitectónicas importantes mediante ADR.

La documentación debe reflejar el comportamiento real del código.

### Paso 10. Verificar y reportar

Antes de finalizar:

- Ejecuta las pruebas disponibles.
- Ejecuta la compilación o verificación equivalente usando el sistema de construcción real del repositorio.
- Comprueba que no se hayan introducido errores de compilación.
- Revisa los cambios y elimina código innecesario.
- Indica los archivos modificados.
- Resume el comportamiento implementado.
- Reporta las pruebas ejecutadas y sus resultados reales.
- Expón cualquier limitación o tarea pendiente.

Nunca afirmes que una prueba pasó si no se ejecutó o no pudiste comprobar su resultado.

## 5. Persistencia y transacciones

TeenSport utiliza una base de datos relacional y Spring Data JPA como mecanismo principal de persistencia.

- Utiliza entidades y repositorios coherentes con el modelo entidad-relación aprobado.
- Prefiere consultas derivadas de Spring Data JPA o JPQL cuando sean suficientes.
- Utiliza SQL nativo solamente cuando la complejidad o el rendimiento lo justifiquen.
- Protege la integridad de las relaciones y las restricciones de datos.
- Usa transacciones cuando una operación de negocio requiera atomicidad.
- No asumas nombres de tablas, columnas o relaciones que no estén definidos en el MER.
- No modifiques el esquema de la base de datos sin justificar el cambio.

## 6. Alcance funcional

El alcance documentado se organiza en tres procesos: registro, autenticación y gestión de usuarios (incluida la recuperación de contraseña); inscripción y matrícula (disciplinas, grupos, horarios, cupos, documentación, pagos presenciales y estado de pago); y entrenamientos y asistencia. Incluye además informes e indicadores y notificaciones por correo.

No implementes por iniciativa propia:

- Pasarela de pagos en línea.
- Nómina.
- Inventario de implementos deportivos.
- Eventos o torneos deportivos, internos o externos.

El reporte de un pago presencial no debe convertirse automáticamente en una integración de pago en línea.

## 7. Restricciones

- No reemplaces la arquitectura aprobada por preferencias personales.
- No inventes requisitos ni relaciones del modelo de datos.
- No modifiques archivos no relacionados con la tarea sin necesidad.
- No incluyas secretos, credenciales ni tokens reales en el código.
- No elimines pruebas para hacer pasar la compilación.
- No agregues dependencias sin revisar si ya existe una alternativa en el proyecto.
- No ejecutes operaciones destructivas sobre datos o bases de datos reales.
- No continúes con una decisión de seguridad o negocio ambigua que pueda afectar la integridad o confidencialidad de los datos.

## 8. Criterios de finalización

Una funcionalidad se considera terminada cuando:

- Cumple el requisito documentado.
- Respeta la arquitectura por capas.
- Incluye las validaciones necesarias.
- Aplica autenticación y autorización cuando corresponda.
- Maneja los errores esperados.
- Mantiene la integridad de los datos.
- Tiene pruebas apropiadas.
- Compila y pasa las verificaciones ejecutadas.
- Incluye la documentación necesaria.
- No introduce cambios ajenos al alcance.
- Se informa cualquier aspecto que no haya podido verificarse.