# 03. Requisitos no funcionales y calidad

La fuente normativa de valores, fórmulas y criterios de aceptación es el Plan de Gestión de la Calidad del Proyecto. Esta documentación resume las metas conocidas y no reemplaza la tabla original.

## Metas de calidad relevantes
- Rendimiento: confirmación promedio de matrícula inferior a 3 segundos (RNF-04).
- Operación: registro de asistencia del grupo en menos de 3 minutos (RNF-05).
- Rendimiento: consultas promedio inferiores a 3 segundos (RNF-06).
- Operación de torneos promedio inferior a 3 segundos (RNF-07), cuando el módulo de torneos esté definido.
- Usabilidad: SUS igual o superior a 70 (RNF-08; principalmente frontend).
- Integridad de datos entre procesos: 100% (RNF-11).
- Manejo de operaciones inválidas: 100% de los casos probados deben rechazarse o gestionarse con mensajes controlados (RNF-13).
- Notificaciones: al menos 95% de envíos esperados exitosos (RNF-14).
- Seguridad: 100% de accesos no autorizados probados bloqueados (RNF-15).
- JWT: 100% de tokens probados deben expirar correctamente (RNF-16).
- Documentación: al menos 80% de clases y métodos según la métrica Javadoc del documento (RNF-17).

## Controles técnicos recomendados
- Pruebas automatizadas asociadas a los requisitos y a los endpoints.
- Transacciones y restricciones de integridad para procesos que actualicen varias entidades.
- Pruebas de concurrencia en reserva de cupos y cambios de grupo.
- Pruebas de autorización por rol y por propiedad del recurso.
- Pruebas de expiración JWT con reloj controlado cuando sea posible.
- Medición de rendimiento con datos representativos; no declarar cumplimiento sin medición.
- Métricas y logs sin contraseñas, JWT, hashes ni datos personales innecesarios.
- Revisión de accesibilidad y usabilidad para la SPA; estas metas no se cumplen solo con backend.

## Trazabilidad de calidad
Cada prueba debe referenciar el ID de requisito no funcional aplicable, registrar resultado y guardar evidencia reproducible. Consultar el plan original para las métricas RNF-01–RNF-03 y para las fórmulas exactas de medición.
