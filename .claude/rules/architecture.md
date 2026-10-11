# Reglas de arquitectura del backend

## Principios

- Aplicar la arquitectura por capas definida en `docs/05-arquitectura-backend.md`.
- Mantener una única responsabilidad principal por clase y método.
- Mantener bajo acoplamiento y alta cohesión.
- Evitar dependencias circulares.
- No incorporar microservicios ni arquitectura hexagonal sin aprobación.

## Responsabilidades

- Controller: HTTP, validación de entrada y presentación de respuestas.
- Service: reglas de negocio, coordinación de casos de uso y transacciones.
- Repository: persistencia y consultas.
- DTO: contratos de entrada y salida.
- Entity: modelo de persistencia.
- Mapper: conversión entre representaciones.
- Integration: adaptadores para servicios externos.

## Restricciones

- Los controllers no deben contener reglas de negocio.
- Los repositories no deben implementar casos de uso.
- Las entidades no deben ser el contrato público de la API.
- No duplicar reglas de negocio entre controllers y services.
- No crear interfaces e implementaciones separadas por costumbre; hacerlo cuando exista una necesidad real de abstracción.
- Los cambios deben seguir las convenciones existentes del proyecto.

Antes de añadir una nueva capa, patrón o abstracción, justificar su necesidad y evaluar si una solución más sencilla cumple el requisito.