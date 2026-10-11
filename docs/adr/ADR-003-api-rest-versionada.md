# ADR-003: API REST consumida por un frontend SPA

- **Estado:** Aceptada.
- **Contexto:** Angular requiere consumir funcionalidades desacopladas del backend.
- **Decisión:** Exponer API REST HTTP bajo `/api/v1`, documentada en OpenAPI.
- **Alternativas:** GraphQL, gRPC, vistas MVC server-side.
- **Justificación:** REST ofrece interoperabilidad y contratos claros entre frontend y backend.
- **Consecuencias:** Los cambios deben reflejarse en OpenAPI, pruebas de contrato y documentación. Respetar los códigos HTTP y códigos de error estables.
