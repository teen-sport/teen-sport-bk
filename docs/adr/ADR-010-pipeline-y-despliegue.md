# ADR-010: CI/CD con Jenkins y despliegue previsto en Azure

- **Estado:** Propuesto en el documento de arquitectura; confirmar vigencia antes de implementarlo.
- **Contexto:** Se requiere automatizar build, pruebas y despliegue de frontend, backend y base de datos administrada.
- **Decisión documentada:** Jenkins para CI/CD y Azure como plataforma prevista (App Service para backend, servicio de base de datos relacional y Static Web Apps para frontend).
- **Alternativas:** GitHub Actions; AWS; GCP.
- **Justificación:** El documento menciona flexibilidad de Jenkins y Azure for Students.
- **Consecuencias:** Mantener Jenkins, controlar consumo de créditos, gestionar secretos mediante variables de entorno. Por ahora hay una única configuración general, sin separación de ambientes. No desplegar ni aprovisionar recursos sin revisar configuración y permisos vigentes.
