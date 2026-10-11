# 01. Contexto del proyecto TeenSport

## Propósito
TeenSport es un sistema web para apoyar la operación de una academia deportiva multidisciplinaria. Centraliza usuarios, deportistas, disciplinas, grupos, matrículas, pagos presenciales reportados, sesiones, asistencia e informes, reduciendo la dependencia de registros manuales y comunicaciones dispersas.

## Actores y roles técnicos
- `ADMIN`: administra usuarios de personal, deportistas cuando actúa en nombre de un acudiente, disciplinas, grupos, horarios, matrículas, pagos presenciales e informes administrativos.
- `COACH`: consulta los grupos asignados, gestiona sesiones dentro de su ámbito autorizado y registra asistencia.
- `GUARDIAN`: administra el perfil de sus deportistas, consulta disponibilidad, solicita matrículas y consulta sus pagos e información de asistencia.

Los nombres de roles del contrato API son `ADMIN`, `COACH` y `GUARDIAN`. No usar `ADMINISTRADOR`, `ENTRENADOR` o `ACUDIENTE` como valores de enum en el código salvo que exista una decisión de mapeo explícita.

## Plataforma y arquitectura
- Frontend independiente: Angular SPA.
- Backend: Java con Spring Boot, API REST versionada bajo `/api/v1`.
- Persistencia: base de datos relacional, ORM/repositorios y SQL nativo selectivo.
- Seguridad: JWT y autorización por rol y pertenencia al recurso.
- Correo: servicio Brevo detrás de un adaptador para desacoplar el proveedor.
- Despliegue previsto: Jenkins y Azure, según el documento de arquitectura. Las decisiones de infraestructura deben revisarse antes de desplegar. Por ahora existe una única configuración general, sin separación de ambientes.

## Alcance respaldado por el OpenAPI disponible
El contrato incluye autenticación, usuarios, perfiles de acudientes/entrenadores, deportistas, disponibilidad, matrículas, pagos, disciplinas, grupos, horarios, sesiones, asistencia e informes.

El MER también incluye estructuras de torneos y notificaciones. Sin embargo, el OpenAPI entregado no declara operaciones para gestionar torneos y no presenta un contrato completo de notificaciones. No implementar esos endpoints hasta que se apruebe su contrato.

## Exclusiones documentadas
- Pasarela de pagos en línea.
- Nómina.
- Inventario de implementos deportivos.
- Gestión de torneos externos.

El registro de pagos es presencial/manual: el acudiente puede reportar que pagó, pero solo el administrador registra el pago y actualiza el estado de pago conforme al contrato.

## Fuentes de verdad y precedencia
1. Requisitos aprobados y decisiones de arquitectura del proyecto.
2. `openapi.yaml` para rutas, métodos, esquemas, códigos HTTP, códigos de error y roles del contrato.
3. `docs/MER.pdf` / `04-modelo-entidad-relacion.md` para estructura de persistencia.
4. Código existente para el estado implementado actualmente.

Si estas fuentes difieren, no inventar una conciliación: registrar la discrepancia y solicitar una decisión.

## Convenciones de idioma
El código, nombres de clases, paquetes, rutas, propiedades JSON, enums y códigos de error deben estar en inglés. El texto de presentación al usuario, especialmente `message` en errores y contenido de correos, debe estar en español. El cliente debe tomar decisiones con `code`, nunca analizando `message`.
