# ADR-005: Persistencia transaccional relacional

- **Estado:** Aceptada.
- **Contexto:** Usuarios, deportistas, matrículas, asistencia y pagos tienen relaciones y requieren integridad y consultas confiables.
- **Decisión:** Usar una base de datos relacional como persistencia principal.
- **Alternativas:** NoSQL como almacén principal; archivos.
- **Justificación:** Claves foráneas, restricciones, transacciones y consultas relacionales respaldan consistencia y reportes.
- **Consecuencias:** Diseñar índices y transacciones. Confirmar el motor elegido en configuración/decisión de despliegue; el OpenAPI menciona MySQL 8 al explicar `training_groups`.
- **Actualización (2026-10-11):** el motor elegido es PostgreSQL 18.6. Las referencias a MySQL 8 en el contrato (`training_groups`) quedan sin efecto: desde el 2026-10-13 la tabla física de los grupos es `groups`, como en el MER y en la base desplegada. Por ahora no se usan migraciones versionadas; las entidades JPA mapean el MER.
- **Actualización (2026-10-12):** la aplicación no modifica la estructura de la base por defecto (`ddl-auto=validate`, configurable con `DB_DDL_AUTO`); solo opera sobre los datos. Las tablas de las bases compartidas se crean fuera de la aplicación y se recomienda que su usuario tenga únicamente permisos `SELECT`, `INSERT`, `UPDATE` y `DELETE`. `update` se reserva para bases locales de desarrollo.
