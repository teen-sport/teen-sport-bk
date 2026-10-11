# Reglas de base de datos

- El MER aprobado es la referencia para las entidades y sus relaciones.
- No inventar entidades, atributos, cardinalidades ni restricciones.
- Mantener la correspondencia entre entidades JPA y el modelo relacional.
- Utilizar claves primarias, foráneas, restricciones e índices cuando corresponda al modelo aprobado.
- Usar Spring Data JPA para las operaciones habituales.
- Utilizar SQL nativo únicamente cuando una necesidad de rendimiento o complejidad lo justifique.
- Documentar la razón de cada consulta nativa importante.
- Evitar consultas N+1 y cargas innecesarias de relaciones.
- Revisar las implicaciones de `FetchType.LAZY` y `FetchType.EAGER`.
- Evitar serializar entidades JPA directamente en la API.
- Utilizar transacciones en operaciones que deben completarse de forma atómica.
- Validar cupos y restricciones de matrícula frente a operaciones concurrentes.
- Evitar actualizaciones destructivas de datos sin autorización explícita.
- Mantener los scripts de esquema y migración bajo control de versiones según la herramienta elegida.
- Probar las restricciones de integridad y los escenarios de concurrencia relevantes.