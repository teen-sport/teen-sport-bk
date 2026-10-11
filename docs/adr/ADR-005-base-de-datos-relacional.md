# ADR-005: Persistencia transaccional relacional

- **Estado:** Aceptada.
- **Contexto:** Usuarios, deportistas, matrículas, asistencia y pagos tienen relaciones y requieren integridad y consultas confiables.
- **Decisión:** Usar una base de datos relacional como persistencia principal.
- **Alternativas:** NoSQL como almacén principal; archivos.
- **Justificación:** Claves foráneas, restricciones, transacciones y consultas relacionales respaldan consistencia y reportes.
- **Consecuencias:** Diseñar índices y transacciones. Confirmar el motor elegido en configuración/decisión de despliegue; el OpenAPI menciona MySQL 8 al explicar `training_groups`.
- **Actualización (2026-10-11):** el motor elegido es PostgreSQL; la versión está pendiente de confirmación del equipo. Las referencias a MySQL 8 en el contrato (`training_groups`) se conservan como nombre físico de la tabla. Las migraciones versionadas quedan pospuestas hasta que se necesite la base de datos.
