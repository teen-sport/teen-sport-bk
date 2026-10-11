# 10. Plan de implementación incremental

El orden busca construir primero las capacidades de las que dependen los módulos de negocio. Cada etapa debe cerrar con compilación, pruebas y documentación actualizada antes de avanzar.

## Fase 0 — Preparación del repositorio
- Confirmar versión de Java/Spring Boot, sistema de construcción y motor relacional.
- Definir profiles y configuración segura por entorno.
- Añadir migraciones versionadas, formato de error, validación, OpenAPI y pruebas base.
- Validar el contrato OpenAPI y decidir cuál archivo saneado será fuente de verdad.

**Estado (2026-10-11): parcial.**
- Hecho: Java 21 / Spring Boot 4.1.1 / Gradle confirmados; motor PostgreSQL (versión pendiente). Paquete `com.uniquindio.teen_sport` conservado; prefijo `/api/v1` en `config/WebConfig`; `HelloController` eliminado. Dependencias `spring-boot-starter-validation` y `springdoc-openapi-starter-webmvc-ui` 3.1.1. Un único ambiente general en `application.properties` (decisión: la configuración por ambientes se hará después y no se crean archivos `-dev`/`-prod`); mensajes de validación en español. Formato `Error`, `ErrorCode`, `BusinessException` y `GlobalExceptionHandler` (400, 404, errores de negocio). Pruebas base en verde.
- Pospuesto por decisión: migraciones versionadas, JPA y driver PostgreSQL hasta conocer la versión del motor.
- Pendiente: confirmar con `openapi.yaml` los códigos de error para 401 y 500 (no figuran en `docs/07`) y añadir entonces el manejador de errores inesperados.

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
