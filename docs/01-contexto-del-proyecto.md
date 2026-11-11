# 01. Contexto del proyecto TeenSport

## Propósito
TeenSport es un sistema web para apoyar la operación de una academia deportiva multidisciplinaria (adolescentes de 12 a 17 años; unos 60 deportistas, 6 entrenadores y grupos de máximo 15). Centraliza usuarios, deportistas, disciplinas, grupos, matrículas, pagos presenciales reportados, sesiones, asistencia e indicadores, reduciendo la dependencia de formularios en papel, planillas físicas y grupos de WhatsApp.

## Procesos de negocio
La Definición del Proyecto (versión 03, 9/11/2026) establece tres procesos relacionados de forma transaccional: la salida de uno es la entrada del siguiente.

| Proceso | Objetivo | Depende de | Alimenta a |
|---|---|---|---|
| Proceso 3 — Registro, autenticación y gestión de usuarios | Registrar, autenticar y administrar las cuentas (administradores, entrenadores y acudientes) y los perfiles de los deportistas a su cargo, con acceso según el rol. | — (proceso habilitador) | Procesos 1 y 2 |
| Proceso 1 — Gestión de Inscripción y Matrícula | Formalizar la vinculación de un deportista a una o varias disciplinas en un periodo determinado, con cupo, grupo, horario y estado de pago. | Proceso 3 | Proceso 2 |
| Proceso 2 — Gestión de Entrenamientos y Asistencia | Programar las sesiones de cada disciplina y registrar de forma centralizada la asistencia. | Procesos 1 y 3 | — |

El Proceso 3 reemplazó al anterior "Gestión de Eventos y Torneos Internos" (versión 02 de la Definición y del Plan de Calidad). Toda referencia a torneos, eventos, equipos, partidos o resultados queda sin efecto.

## Actores y roles técnicos
- `ADMIN`: cubre al coordinador académico deportivo y al auxiliar administrativo. Crea y administra las cuentas de entrenadores y administradores (rol, activación e inactivación), activa o inactiva cuentas de acudientes, gestiona disciplinas, cupos, grupos y horarios, confirma matrículas, actualiza manualmente el estado de pago y consulta reportes e indicadores de toda la operación.
- `COACH`: programa y consulta las sesiones de sus grupos, registra la asistencia de los deportistas a su cargo y consulta reportes de asistencia de sus grupos.
- `GUARDIAN`: se autorregistra, crea y administra los perfiles de sus deportistas, los inscribe y matricula, reporta el pago presencial de la mensualidad y consulta el estado de pago y la asistencia.

El deportista no es usuario del sistema ni tiene vista propia: su perfil lo crea y administra su acudiente. Los roles técnicos son únicamente `ADMIN`, `COACH` y `GUARDIAN`; no usar `ADMINISTRADOR`, `ENTRENADOR` o `ACUDIENTE` como valores de enum salvo que exista una decisión de mapeo explícita.

> El SAD (sección 1.4) menciona "deportista" en la asignación de roles. Se sigue la Definición del Proyecto (el deportista no es usuario) hasta que el SAD se corrija.

## Plataforma y arquitectura
- Frontend independiente: Angular SPA responsive.
- Backend: Java con Spring Boot, API REST versionada bajo `/api/v1`.
- Persistencia: base de datos relacional (PostgreSQL), ORM/repositorios y SQL nativo selectivo.
- Seguridad: JWT y autorización por rol y pertenencia al recurso.
- Correo: servicio Brevo detrás de un componente de notificaciones y un adaptador, para desacoplar el proveedor.
- CI/CD y nube (ADR-10): repositorio en GitHub, GitHub Actions para integración y despliegue continuo, y Railway para alojar el backend, el frontend y la base de datos. Existe una única configuración general, sin separación de ambientes.

## Alcance
Incluye, según la Definición del Proyecto y el SAD:
- Registro y autenticación de usuarios por rol, incluida la recuperación de contraseña por correo, la activación e inactivación de cuentas y la gestión de los perfiles de los deportistas por su acudiente.
- Inscripción y matrícula en las disciplinas, con verificación de cupo y asignación de grupo y horario. La documentación de la matrícula (autorización del acudiente y certificado médico) queda fuera del alcance actual por decisión del equipo (2026-11-09).
- Registro y actualización manual del estado de pago (al día / en mora) por el administrador, según el pago presencial.
- Programación de sesiones y registro de asistencia por el entrenador.
- Consulta de asistencia, reportes e indicadores por rol.
- Notificaciones por correo sobre registro de cuenta, recuperación de contraseña, inscripciones, horarios y novedades.

### Estado de la entrega actual (Entrega 3)
- Desarrollados: Procesos 1, 2 y 3 en sus operaciones principales (HU28–HU39 integradas en `development`).
- Nota de alcance de los documentos oficiales (Definición v03, SAD v03 y Plan de Calidad): el envío de correos/notificaciones (Brevo), la recuperación de contraseña, la creación de administradores desde la interfaz y la generación de reportes quedan planificados para la tercera entrega; siguen siendo parte del alcance del proyecto.
- Estamos en la tercera entrega: los reportes (R1–R4 e indicadores del Proceso 3), el adaptador Brevo con las notificaciones y la recuperación de contraseña están en el alcance actual. El correo y la recuperación de contraseña se hacen al final. La creación de administradores ya existe en la API (U8); la nota se refiere a la interfaz. El adaptador Brevo y las notificaciones se implementaron el 2026-11-10 (etapa 6); falta la recuperación de contraseña (etapa 7).
- El orden del trabajo restante está en `10-plan-implementacion.md`, sección "Entrega 3".
- Mientras no exista el correo, las funcionalidades que dependan del correo deben dejar definido su punto de notificación sin simular envíos.

## Exclusiones documentadas
- Nómina y contratación del personal.
- Inventario de implementos deportivos.
- Procesamiento electrónico de pagos (pasarela en línea): la mensualidad se paga de forma presencial y el sistema solo refleja el estado de pago.
- Organización de eventos o torneos deportivos, internos o con participación de personas o academias externas.

El registro de pagos es presencial/manual: el acudiente puede reportar que pagó, pero solo el administrador registra el pago y actualiza el estado de pago conforme al contrato.

## Fuentes de verdad y precedencia
1. Documentos oficiales del proyecto: Definición del Proyecto (v03, 9/11/2026), Plan de Gestión de la Calidad (v02, 19/10/2026) y Software Architecture Description (v03, 9/11/2026) con sus ADR.
2. `openapi.yaml` para rutas, métodos, esquemas, códigos HTTP, códigos de error y roles del contrato.
3. `docs/MER.pdf` / `04-modelo-entidad-relacion.md` para estructura de persistencia.
4. Código existente para el estado implementado actualmente.

Cuando el contrato, el MER o el código difieran de los documentos oficiales, se adapta el proyecto a los documentos oficiales. Las contradicciones internas de los documentos oficiales se registran en `02-requisitos-funcionales.md` y se resuelven con los responsables antes de implementar.

## Convenciones de idioma
El código, nombres de clases, paquetes, rutas, propiedades JSON, enums y códigos de error deben estar en inglés. El texto de presentación al usuario, especialmente `message` en errores y contenido de correos, debe estar en español. El cliente debe tomar decisiones con `code`, nunca analizando `message`.
