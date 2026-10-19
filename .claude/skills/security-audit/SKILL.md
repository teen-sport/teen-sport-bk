---
name: security-audit
description: Audita la seguridad del backend de TeenSport, incluyendo JWT, autenticación, autorización por roles, acceso a recursos, contraseñas, validación de entradas, protección de datos de menores y configuración de endpoints. Utilizar para revisiones de seguridad y antes de liberar cambios sensibles.
---

# Auditoría de seguridad del backend de TeenSport

## 1. Objetivo

Identificar y documentar vulnerabilidades y riesgos de seguridad en el backend de TeenSport, proponiendo correcciones concretas y verificables.

La auditoría debe comprobar el comportamiento real del código y de su configuración. No debe considerar una anotación, una clase de seguridad o una prueba aislada como evidencia suficiente de que el sistema está protegido.

La revisión debe realizarse de forma controlada, sin acceder a sistemas ajenos ni ejecutar operaciones destructivas.

## 2. Contexto y datos protegidos

TeenSport es una academia deportiva multidisciplinaria cuyo sistema contempla los siguientes roles:

- ADMINISTRADOR.
- ENTRENADOR.
- ACUDIENTE.

El sistema administra información relacionada con usuarios, deportistas menores de edad, inscripciones, disciplinas, grupos, horarios, documentación de la matrícula, asistencia, pagos reportados e informes.

La información personal de los menores requiere especial protección. El diseño debe tener en cuenta los principios y obligaciones aplicables de la Ley 1581 de 2012 y demás normativa colombiana pertinente, sin asumir que una medida técnica por sí sola garantiza el cumplimiento legal.

## 3. Fuentes de revisión

Consulta:

- `CLAUDE.md`.
- `.claude/rules/security.md`.
- `.claude/rules/architecture.md`.
- `.claude/rules/error-handling.md`.
- `docs/02-requisitos-funcionales.md`.
- `docs/03-requisitos-no-funcionales.md`.
- `docs/04-modelo-entidad-relacion.md`.
- `docs/05-arquitectura-backend.md`.
- `docs/06-seguridad-y-autorizacion.md`.
- `docs/07-contrato-api.md`.
- Las configuraciones de Spring Security, autenticación, JWT y gestión de secretos.
- Los controladores, servicios, repositorios y DTO relacionados con la operación auditada.
- Las pruebas de seguridad existentes.

Si algún documento no existe o está incompleto, utiliza únicamente la información que sí esté disponible y registra las limitaciones.

## 4. Autenticación y JWT

Inspecciona el flujo completo de autenticación.

### 4.1. Emisión del token

Comprueba que:

- La identidad del usuario se determine a partir de credenciales verificadas.
- La contraseña no se almacene ni se compare como texto plano.
- El token se emita únicamente después de autenticar correctamente al usuario.
- Se utilice una clave o mecanismo criptográfico adecuado.
- La expiración del token esté configurada y se aplique efectivamente.
- El algoritmo de firma no pueda ser elegido libremente por una entrada no confiable.
- No se incluyan datos personales innecesarios en el token.
- Los tokens no se registren en logs ni se expongan en mensajes de error.

### 4.2. Validación del token

Comprueba que el backend rechace tokens:

- Ausentes en endpoints protegidos.
- Malformados.
- Con firma inválida.
- Expirados.
- Con algoritmos o parámetros no permitidos.
- Con claims obligatorios ausentes o inválidos, cuando correspondan.
- Emitidos para un contexto o destinatario distinto, si el diseño utiliza tales restricciones.

Verifica también cómo se obtiene el usuario autenticado y cómo se gestiona un usuario que deja de estar habilitado.

No presupongas que JWT permite revocar inmediatamente cualquier token. Si el proyecto requiere revocación anticipada, analiza y documenta el mecanismo aprobado para conseguirla.

### 4.3. Manejo de tokens

- No guardes tokens en repositorios, archivos de ejemplo con credenciales reales ni logs.
- No aceptes identidades o roles enviados por el cliente como sustituto de los claims validados.
- Comprueba que el manejo de errores de autenticación no revele información sensible.
- Revisa que las respuestas de autenticación sean coherentes y no expongan detalles innecesarios sobre cuentas existentes.

## 5. Autorización por roles y recursos

### 5.1. Control por rol

Revisa la configuración de Spring Security y los mecanismos de autorización utilizados por los endpoints y métodos de servicio.

Verifica que:

- Los endpoints protegidos no permitan acceso anónimo.
- Los roles permitidos estén definidos de acuerdo con los requisitos.
- Las operaciones administrativas estén restringidas a ADMINISTRADOR.
- Las operaciones de asistencia estén limitadas a los entrenadores autorizados.
- Las operaciones de acudientes respeten sus permisos documentados.
- Los endpoints de informes no expongan información a actores no autorizados.
- Las reglas de autorización sean consistentes entre la configuración HTTP y las restricciones a nivel de método, cuando ambas se utilicen.

No asumas permisos que no estén documentados. Si un caso de uso no especifica los roles permitidos, regístralo como una definición pendiente.

### 5.2. Autorización sobre registros

Además del rol, verifica que el usuario tenga permiso para acceder al registro solicitado.

Ejemplos de controles que deben evaluarse según las reglas del negocio:

- Un acudiente no debe poder consultar o modificar información de deportistas que no están bajo su responsabilidad autorizada.
- Un entrenador no debe poder registrar asistencia para grupos o sesiones fuera de su ámbito autorizado.
- Un usuario no administrativo no debe poder cambiar su propio rol enviando un campo modificado.
- La modificación de un identificador en la URL no debe permitir acceder a recursos de otro usuario.
- Un usuario no debe poder inscribir deportistas ajenos ni manipular sus estados de pago sin autorización.
- La consulta de informes debe limitarse al ámbito autorizado del solicitante.

Los ejemplos son escenarios de auditoría, no sustituyen la definición formal de las reglas del proyecto.

## 6. Contraseñas y cuentas

Verifica que:

- Las contraseñas se almacenen mediante un algoritmo de hash adaptativo y apropiado, como BCrypt o una alternativa aprobada.
- El proceso de autenticación use el mecanismo de verificación correspondiente.
- Nunca se almacenen contraseñas en texto plano.
- Las contraseñas no se incluyan en DTO de respuesta, logs ni mensajes de error.
- La entidad persistente no se exponga directamente de manera que filtre el hash.
- La creación y modificación de usuarios apliquen las validaciones establecidas.
- Los cambios de rol estén restringidos y validados.
- Los mensajes de autenticación no faciliten innecesariamente la enumeración de cuentas.

No inventes una política de longitud o complejidad de contraseñas si no está aprobada. Identifica la política definida o solicita que se establezca.

## 7. Validación de entradas

Revisa la validación en las fronteras de entrada de la aplicación.

Comprueba:

- Validaciones de DTO mediante Bean Validation cuando corresponda.
- Validación de campos obligatorios, formatos, tamaños y rangos.
- Reglas de negocio que no pueden expresarse únicamente con anotaciones.
- Validación de identificadores y referencias a entidades existentes.
- Restricciones sobre campos que un cliente no debería poder modificar.
- Tratamiento de valores nulos, listas vacías y combinaciones inválidas.
- Validación de parámetros de paginación, filtros y ordenamiento.
- Uso seguro de parámetros en las consultas.
- Ausencia de confianza ciega en datos procedentes del cliente.

Las validaciones de entrada no reemplazan las restricciones de autorización ni las restricciones de integridad de la base de datos.

## 8. Protección de datos de menores

Verifica que:

- Solo los actores autorizados puedan consultar información personal.
- Los DTO expongan únicamente los campos necesarios para cada caso de uso.
- Los informes y consultas no filtren datos de otros grupos o familias.
- Los logs eviten datos personales innecesarios.
- Las respuestas de error no expongan información privada.
- Los datos no se envíen a servicios externos sin una necesidad funcional aprobada.
- Las integraciones de correo reciban únicamente los datos necesarios para la notificación.
- La información sensible no aparezca en ejemplos, datos de prueba compartidos o archivos versionados.

Identifica los riesgos técnicos y las decisiones que requieren validación legal o administrativa. No declares cumplimiento legal completo basándote únicamente en la revisión del código.

## 9. Seguridad de la API y configuración

Revisa:

- Configuración de CORS por orígenes aprobados.
- Reglas de autorización por defecto.
- Gestión segura de secretos y variables de entorno.
- Configuración de errores HTTP.
- Exposición de Swagger/OpenAPI.
- Protección de endpoints de administración.
- Restricciones de métodos HTTP.
- Uso de consultas parametrizadas o mecanismos seguros de persistencia.
- Dependencias con vulnerabilidades conocidas, cuando existan herramientas o información disponibles.
- Configuraciones de desarrollo que puedan quedar habilitadas en producción.
- Ausencia de endpoints de depuración que revelen información sensible.

No des por seguro un endpoint porque use HTTPS o JWT. Examina también la autorización, la validación de datos y las reglas de acceso a los recursos.

## 10. Transacciones e integridad

Identifica operaciones que puedan dejar información inconsistente cuando ocurren errores.

Presta atención a:

- Inscripciones y capacidad de los grupos.
- Registro de asistencia.
- Cambios en estados de pago.
- Registro de cuentas con correo o documento duplicado.
- Recuperación de contraseña y vigencia de sus tokens.
- Actualizaciones concurrentes.
- Operaciones con varias escrituras.
- Notificaciones enviadas antes de confirmar una transacción.

Evalúa si se necesitan transacciones, restricciones de base de datos, control de concurrencia u otros mecanismos. No propongas cambios sin explicar el riesgo concreto y su relación con las reglas de negocio.

## 11. Pruebas de seguridad

Comprueba como mínimo los escenarios aplicables:

- Solicitud sin token a un endpoint protegido.
- Token inválido.
- Token expirado.
- Usuario autenticado sin el rol requerido.
- Usuario con rol válido que intenta acceder a un recurso ajeno.
- Intento de modificación no autorizada de roles.
- Entrada inválida.
- Acceso a información personal fuera del ámbito permitido.
- Exposición accidental de credenciales, tokens o hashes.
- Comportamiento ante errores inesperados.

Utiliza usuarios y datos ficticios. No pruebes contra producción ni realices cambios destructivos.

El requisito de calidad de TeenSport establece el bloqueo de todos los accesos no autorizados probados y la expiración correcta de todos los JWT probados. Consulta la especificación original para respetar su criterio de medición.

## 12. Priorización de hallazgos

Clasifica los resultados como:

- **Crítica:** vulnerabilidad grave y demostrada que permite comprometer datos o funciones sensibles.
- **Alta:** acceso no autorizado relevante, exposición importante de información o fallo grave de protección.
- **Media:** debilidad que requiere condiciones adicionales o afecta un conjunto limitado de operaciones.
- **Baja:** riesgo limitado o mejora de endurecimiento.
- **Informativa:** observación o pregunta que requiere una decisión o mayor evidencia.

Explica el impacto y las condiciones necesarias para reproducir cada problema.

## 13. Formato de salida

### Resumen ejecutivo

- Alcance de la auditoría.
- Componentes examinados.
- Riesgos principales.
- Limitaciones de la revisión.

### Hallazgos

Para cada hallazgo, incluye:

1. Identificador: SEC-001, SEC-002, etc.
2. Severidad.
3. Archivo y método involucrados.
4. Evidencia técnica.
5. Escenario de riesgo.
6. Impacto sobre confidencialidad, integridad o disponibilidad.
7. Corrección recomendada.
8. Prueba para verificar la corrección.
9. Requisito relacionado, si existe.

No incluyas valores reales de secretos o credenciales en el informe.

### Pruebas ejecutadas

Indica las pruebas realizadas, los resultados observados y las pruebas pendientes.

### Decisiones pendientes

Enumera las políticas de seguridad o reglas de negocio que necesitan aprobación.

### Plan de remediación

Ordena las correcciones por prioridad e indica las dependencias entre ellas.

## 14. Restricciones

- No realices pruebas destructivas.
- No ataques sistemas externos.
- No uses datos personales reales para pruebas.
- No expongas credenciales encontradas.
- No modifiques archivos sin autorización para corregir.
- No inventes vulnerabilidades ni afirmes que un ataque es posible sin evidencia suficiente.
- No declares que el sistema es completamente seguro.
- No declares cumplimiento legal completo.
- No confundas autenticación correcta con autorización suficiente.
- No consideres una auditoría estática como sustituto de las pruebas dinámicas cuando estas sean necesarias.