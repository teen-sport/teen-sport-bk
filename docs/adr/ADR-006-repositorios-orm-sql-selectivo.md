# ADR-006: Repositorios con ORM y SQL nativo selectivo

- **Estado:** Aceptada.
- **Contexto:** Se busca evitar SQL disperso en lógica de negocio sin perder rendimiento en consultas complejas.
- **Decisión:** Encapsular acceso a datos en repositorios; utilizar Spring Data JPA/ORM por defecto y SQL nativo solo con justificación.
- **Alternativas:** SQL directo para todo; ORM sin excepción.
- **Justificación:** Mantiene la mayoría de operaciones legibles y permite optimizar reportes o consultas costosas.
- **Consecuencias:** Toda consulta nativa debe documentar su razón, parámetros, índices/plan cuando proceda y pruebas de integración.
