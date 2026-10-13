---
name: review-backend
description: Revisa cambios y código existente del backend de TeenSport para detectar defectos funcionales, problemas de arquitectura, malas prácticas, errores de persistencia, deficiencias de pruebas y riesgos de mantenibilidad. Utilizar antes de integrar cambios o al solicitar una revisión de código.
---

# Revisión de código del backend de TeenSport

## 1. Objetivo

Realizar una revisión técnica objetiva del backend de TeenSport, priorizando la corrección funcional, la seguridad, la integridad de los datos, el cumplimiento de los requisitos y la mantenibilidad.

La revisión debe identificar problemas demostrables y explicar cómo corregirlos.

No reescribas el código automáticamente. Primero presenta los hallazgos y sus evidencias. Modifica los archivos únicamente si el usuario solicita las correcciones o autoriza expresamente la remediación.

## 2. Fuentes que debes consultar

Consulta `CLAUDE.md`, las reglas de `.claude/rules/` y la documentación disponible en `docs/`.

Prioriza especialmente:

- Requisitos funcionales.
- Requisitos no funcionales.
- Modelo entidad-relación.
- Arquitectura del backend.
- Seguridad y autorización.
- Contrato de la API.
- Estrategia de pruebas.
- Decisiones arquitectónicas registradas en ADR.

Si no tienes acceso a una fuente necesaria, indícalo. No afirmes que un cambio incumple una regla que no has podido verificar.

## 3. Alcance de la revisión

### 3.1. Corrección funcional

Verifica que:

- La implementación satisfaga el requisito asociado.
- Las reglas de negocio estén en la capa apropiada.
- Se contemplen los casos límite relevantes.
- Los estados y transiciones sean válidos.
- No existan condiciones que permitan duplicar operaciones indebidamente.
- Las respuestas HTTP representen correctamente el resultado.
- Las operaciones fallidas no se reporten como exitosas.

No inventes reglas de negocio para declarar un defecto. Si una regla no está definida, regístrala como una pregunta o ambigüedad.

### 3.2. Arquitectura por capas

Verifica que:

- Los controladores no contengan reglas de negocio complejas.
- Los servicios coordinen los casos de uso.
- Los repositorios se concentren en persistencia.
- Las entidades no se expongan indiscriminadamente como respuestas de la API.
- Los DTO representen adecuadamente las entradas y salidas.
- Las dependencias fluyan de forma coherente.
- No existan ciclos de dependencias evitables.
- No se introduzcan microservicios ni arquitectura hexagonal sin aprobación.
- Las abstracciones tengan una finalidad concreta.

### 3.3. Calidad del código

Busca:

- Métodos con demasiadas responsabilidades.
- Clases excesivamente grandes.
- Código duplicado.
- Nombres ambiguos.
- Condicionales complejos que puedan simplificarse.
- Parámetros o variables innecesarios.
- Captura incorrecta de excepciones.
- Uso excesivo de valores mágicos.
- Dependencias innecesarias.
- Código muerto o inalcanzable.
- Mutabilidad innecesaria.
- Comentarios desactualizados o que contradigan la implementación.

No marques una preferencia de estilo como defecto crítico si no afecta la corrección, la seguridad o el mantenimiento.

### 3.4. Seguridad

Verifica:

- Configuración de autenticación y autorización.
- Validación de firma, expiración y demás propiedades exigidas para los JWT.
- Restricción de endpoints por rol.
- Control de acceso a recursos según su propietario o relación con el actor.
- Validación de entradas.
- Almacenamiento seguro de contraseñas mediante un algoritmo de hash apropiado.
- Ausencia de contraseñas y tokens en logs o respuestas.
- Ausencia de secretos en el repositorio.
- Riesgos de asignación masiva de propiedades.
- Exposición de datos personales de menores.
- Validación de los datos usados para construir consultas.
- Manejo de CORS según los orígenes aprobados.

No consideres suficiente que un endpoint tenga `authenticated()` o que el usuario posea un rol general si también debe comprobarse que el recurso pertenece a ese usuario o está dentro de su ámbito de acceso.

### 3.5. Persistencia e integridad

Verifica:

- Correspondencia entre entidades, relaciones y MER.
- Restricciones de integridad.
- Uso correcto de transacciones.
- Consultas innecesariamente complejas o ineficientes.
- Posibles problemas N+1.
- Carga de relaciones y riesgo de ciclos de serialización.
- Operaciones que puedan perder actualizaciones o producir duplicados.
- Coherencia entre el estado persistido y el resultado comunicado al cliente.

No declares que una consulta es lenta sin evidencia o un análisis que respalde esa conclusión.

### 3.6. Manejo de errores

Verifica:

- Validación de errores esperados.
- Uso coherente de excepciones.
- Existencia y comportamiento del manejador global de errores.
- Códigos HTTP adecuados.
- Mensajes útiles y seguros.
- Ausencia de trazas internas y detalles sensibles en las respuestas.
- Ausencia de excepciones ignoradas.
- Conservación de la causa original cuando sea relevante para el diagnóstico.

### 3.7. Pruebas y documentación

Verifica:

- Cobertura de escenarios exitosos y fallidos.
- Pruebas de reglas de negocio.
- Pruebas de autorización y acceso a recursos.
- Pruebas de validación y manejo de errores.
- Pruebas de integración cuando sean necesarias.
- Correspondencia entre las pruebas y los requisitos.
- Javadoc de clases y métodos relevantes.
- Actualización de la documentación de endpoints.
- Actualización de la trazabilidad cuando aplique.

La documentación de calidad de TeenSport establece una meta de Javadoc del 80 % para las clases y métodos según la métrica definida. Consulta el documento original para determinar el criterio exacto de medición.

## 4. Severidad de los hallazgos

Clasifica cada hallazgo con una de estas prioridades:

- **Crítica:** riesgo demostrado de acceso no autorizado grave, exposición importante de información sensible o daño severo a la integridad de los datos.
- **Alta:** defecto que compromete una regla de negocio importante, permite un acceso indebido o puede provocar una inconsistencia relevante.
- **Media:** defecto localizado que afecta el comportamiento esperado, la mantenibilidad o el tratamiento de errores.
- **Baja:** mejora localizada de claridad, documentación o estilo sin impacto inmediato importante.
- **Informativa:** observación, riesgo potencial o pregunta que requiere más contexto.

No asignes severidad únicamente por la apariencia del código. Explica el impacto real o el riesgo razonablemente sustentado.

## 5. Formato obligatorio de los hallazgos

Para cada hallazgo incluye:

1. **Identificador:** R-001, R-002, etc.
2. **Severidad.**
3. **Archivo y ubicación:** ruta y método o bloque relevante.
4. **Problema:** qué ocurre.
5. **Evidencia:** fragmento o comportamiento observado.
6. **Impacto:** consecuencia funcional, de seguridad o mantenimiento.
7. **Recomendación:** corrección concreta.
8. **Prueba sugerida:** cómo verificar que el problema queda resuelto.
9. **Requisito relacionado:** si puede identificarse en la documentación.

Distingue entre defectos confirmados, riesgos potenciales y decisiones de diseño discutibles.

## 6. Procedimiento de revisión

1. Identifica los archivos modificados y el alcance de la revisión.
2. Lee el código y sus dependencias relevantes.
3. Consulta los requisitos y decisiones arquitectónicas relacionados.
4. Revisa los posibles defectos por categoría.
5. Ejecuta las pruebas o verificaciones disponibles cuando sea apropiado.
6. Comprueba si los hallazgos tienen evidencia suficiente.
7. Elimina falsos positivos y duplicados.
8. Presenta los resultados ordenados por severidad.
9. Indica qué aspectos no se pudieron verificar.

No afirmes que el backend es completamente seguro ni que cumple todos los requisitos por haber revisado un conjunto limitado de archivos.

## 7. Formato de salida

Organiza el resultado de la revisión así:

### Resumen

- Alcance revisado.
- Estado general de la revisión.
- Riesgos principales.
- Verificaciones ejecutadas.

### Hallazgos

Lista los hallazgos ordenados por severidad, siguiendo el formato establecido.

Si es útil, presenta una tabla con identificador, severidad, archivo y descripción breve, seguida de los detalles.

### Pruebas y verificaciones

Indica comandos ejecutados, resultado observado y pruebas que faltan.

### Aspectos no verificables

Explica las limitaciones de la revisión y la información necesaria para continuar.

### Recomendaciones

Propón el orden sugerido para corregir los hallazgos. No confundas recomendaciones futuras con defectos confirmados.

## 8. Restricciones

- No cambies archivos durante una revisión de solo lectura.
- No inventes hallazgos.
- No confundas preferencias personales con incumplimientos.
- No supongas requisitos no documentados.
- No expongas secretos encontrados; describe el problema sin reproducir el valor sensible.
- No ejecutes operaciones destructivas.
- No declares que una prueba pasó si no fue ejecutada o no se verificó su resultado.
- No marques como cumplido un requisito únicamente porque existe una clase o anotación con un nombre relacionado.