# ADR-005: Persistencia transaccional relacional

- **Estado:** Aceptada.
- **Contexto:** Usuarios, deportistas, matrículas, asistencia y pagos tienen relaciones y requieren integridad y consultas confiables.
- **Decisión:** Usar una base de datos relacional como persistencia principal.
- **Alternativas:** NoSQL como almacén principal; archivos.
- **Justificación:** Claves foráneas, restricciones, transacciones y consultas relacionales respaldan consistencia y reportes.
- **Consecuencias:** Diseñar índices y transacciones. Confirmar el motor elegido en configuración/decisión de despliegue; el OpenAPI menciona MySQL 8 al explicar `training_groups`.
