# ADR-002: Frontend y backend separados, organizados por capas

- **Estado:** Aceptada según la descripción de arquitectura.
- **Contexto:** Sistema de tamaño moderado con roles diferenciados y necesidad de mantener por separado interfaz, lógica de negocio y persistencia.
- **Decisión:** Dos aplicaciones desplegables independientes (Angular y Spring Boot); el backend utiliza arquitectura monolítica por capas.
- **Alternativas:** Monolito full-stack con vistas server-side; microservicios; arquitectura hexagonal.
- **Justificación:** Reduce complejidad operativa y mantiene separación de responsabilidades sin añadir infraestructura innecesaria.
- **Consecuencias:** Se deben mantener contratos API estables, CORS y dos despliegues. No adoptar microservicios ni arquitectura hexagonal sin revisar este ADR.
