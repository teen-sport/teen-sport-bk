# 10. Plan de implementación incremental

El orden busca construir primero las capacidades de las que dependen los módulos de negocio. Cada etapa debe cerrar con compilación, pruebas y documentación actualizada antes de avanzar.

## Fase 0 — Preparación del repositorio
- Confirmar versión de Java/Spring Boot, sistema de construcción y motor relacional.
- Definir profiles y configuración segura por entorno.
- Añadir migraciones versionadas, formato de error, validación, OpenAPI y pruebas base.
- Validar el contrato OpenAPI y decidir cuál archivo saneado será fuente de verdad.

## Fase 1 — Seguridad y cuentas
- Entidades y migraciones para roles, users, guardians y coaches según MER.
- Registro de acudiente y login.
- BCrypt, JWT, contexto autenticado y manejo global de errores.
- Pruebas de roles, token expirado y propiedad de recursos.

## Fase 2 — Deportistas y catálogos
- Perfiles de deportistas.
- Disciplinas y reglas de estado.
- DTO, validaciones y filtros de consulta.

## Fase 3 — Grupos y horarios
- Grupos y asignación de entrenador.
- Capacidad y validación de entrenador/disciplina activos.
- Horarios semanales.

## Fase 4 — Matrículas y disponibilidad
- Disponibilidad por periodo.
- Solicitud de matrícula y control de cupos bajo concurrencia.
- Confirmación, rechazo, cancelación y cambio de grupo.
- Pruebas de transacciones y consistencia.

## Fase 5 — Pagos presenciales
- Reporte por acudiente.
- Registro por administrador e historial.
- Estados de pago conforme al contrato; sin pasarela ni job automático.

## Fase 6 — Sesiones y asistencia
- Creación/generación de sesiones.
- Lista elegible y registro de asistencia.
- Consultas históricas filtradas por rol.

## Fase 7 — Informes y correo
- Informes R1–R4 con consultas probadas.
- Adaptador Brevo desacoplado y notificaciones posteriores al commit.
- Observabilidad y pruebas de fallos del proveedor.

## Fase 8 — Funcionalidades pendientes de contrato
- Definir API de torneos antes de implementarla, ya que el MER contiene entidades no expuestas por OpenAPI.
- Definir API y ciclo de vida de notificaciones si requiere consultas desde el frontend.

## Fase 9 — Calidad y despliegue
- Ejecutar suite completa, revisar cobertura y Javadoc.
- Medir RNF de rendimiento y seguridad.
- Validar CORS, secretos, perfiles y configuración productiva.
- Preparar pipeline Jenkins y despliegue Azure cuando estén aprobados los entornos.
