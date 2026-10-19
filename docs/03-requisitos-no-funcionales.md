# 03. Requisitos no funcionales y calidad

La fuente normativa de valores, fórmulas y criterios de aceptación es el Plan de Gestión de la Calidad del Proyecto (versión 02, 19/10/2026), sección 4. Esta documentación transcribe sus metas y no reemplaza la tabla original.

## Requisitos no funcionales (ISO/IEC 25010)

| ID | Nombre | Proceso | Criterio de aceptación | Prioridad |
|---|---|---|---|---|
| RNF-01 | Exactitud de la matrícula | P1 Inscripción y Matrícula | ≥ 90 % de matrículas de prueba con cupo, grupo, horario y estado de pago correctos. | Alta |
| RNF-02 | Validación de datos de inscripción | P1 | 100 % de entradas inválidas (edad fuera de 12–17, campos obligatorios vacíos, formato incorrecto) rechazadas. | Alta |
| RNF-03 | Corrección funcional del módulo de matrícula | P1 | ≥ 90 % de casos de prueba funcionales superados (registro, verificación de cupo, confirmación, estado de pago). | Alta |
| RNF-04 | Tiempo de respuesta de la inscripción | P1 | Confirmación promedio de matrícula < 3 s. | Alta |
| RNF-05 | Tiempo de registro de asistencia | P2 Entrenamientos y Asistencia | Registro promedio de asistencia por grupo < 3 min. | Alta |
| RNF-06 | Tiempo de respuesta en consultas | P2 | Consulta promedio de horarios o asistencia < 3 s. | Media |
| RNF-07 | Tiempo de respuesta del inicio de sesión | P3 Registro, autenticación y usuarios | Inicio de sesión o registro de cuenta promedio < 3 s. | Media |
| RNF-08 | Facilidad de registro de asistencia | P2 | Puntaje SUS ≥ 70 con entrenadores representativos (frontend). | Alta |
| RNF-09 | Accesibilidad de la interfaz | P2 | Puntaje de accesibilidad de Lighthouse ≥ 70 (frontend). | Media |
| RNF-10 | Cobertura de registro de asistencia | P2 | ≥ 90 % de sesiones programadas con asistencia registrada. | Alta |
| RNF-11 | Unicidad de cuentas de usuario | P3 | 100 % de registros con documento o correo ya registrado rechazados. | Alta |
| RNF-12 | Consistencia de la vinculación de deportistas | P3 | 100 % de perfiles de prueba vinculados correctamente a su acudiente, sin huérfanos ni vinculaciones erróneas. | Alta |
| RNF-13 | Manejo controlado de errores | P3 | 100 % de operaciones inválidas (p. ej., credenciales incorrectas o cuenta inactiva) rechazadas con mensaje controlado. | Alta |
| RNF-14 | Confiabilidad de notificaciones | P3 | ≥ 95 % de notificaciones de registro exitoso y recuperación de contraseña enviadas correctamente. | Media |
| RNF-15 | Control de acceso por rol | P1 (aplicación transversal) | 100 % de accesos indebidos probados bloqueados. | Alta |
| RNF-16 | Vigencia controlada de sesión | Transversal | 100 % de tokens JWT probados expiran en el tiempo configurado sin uso posterior. | Alta |
| RNF-17 | Documentación de código | Transversal | ≥ 80 % de clases y métodos del backend documentados con Javadoc. | Media |

Notas:
- RNF-07 y RNF-11 a RNF-14 se redefinieron en la versión 02 al reemplazar el antiguo proceso de torneos; las metas anteriores (operación de torneos, integridad entre procesos, consistencia de resultados) quedan sin efecto.
- El Plan fija en su sección 3.4.3 la confiabilidad de notificaciones en ≥ 80 %, mientras RNF-14 exige ≥ 95 %. Se aplica RNF-14 hasta que el Plan se corrija.
- RNF-14 depende del adaptador de correo, fuera de la entrega actual; no se puede declarar su cumplimiento sin envíos reales medidos.

## Métricas del proceso de desarrollo (Plan, sección 3.3)
- Por módulo: defectos por módulo (Jira), cobertura de pruebas (JaCoCo), casos de prueba superados y cumplimiento de entregables.
- P1: porcentaje de reglas críticas (cupos, estados de la matrícula y pagos) con prueba automatizada, y escenarios de concurrencia superados (último cupo, misma disciplina, pago del mismo mes).
- P2: porcentaje de reglas de sesiones y asistencia con prueba automatizada (no registrar en sesión futura o cancelada, solo el entrenador del grupo registra, reemplazo de marcas) y endpoints con control de acceso por rol superado.
- P3: casos de prueba de seguridad superados (autenticación, control de acceso por rol y recuperación de contraseña); casos de acceso al sistema rechazados como corresponde (token ausente, alterado o vencido, cuenta inactiva, credenciales incorrectas); reglas de registro de cuentas con prueba automatizada (contraseña débil, política de datos aceptada, correo y documento repetidos).

## Controles técnicos recomendados
- Pruebas automatizadas asociadas a los requisitos y a los endpoints.
- Transacciones y restricciones de integridad para procesos que actualicen varias entidades.
- Pruebas de concurrencia en reserva de cupos, cambios de grupo, pagos del mismo mes y registros de cuentas duplicadas.
- Pruebas de autorización por rol y por propiedad del recurso.
- Pruebas de expiración JWT con reloj controlado cuando sea posible, y de tokens de cuentas inactivadas.
- Medición de rendimiento con datos representativos (unos 60 deportistas y 6 entrenadores); no declarar cumplimiento sin medición.
- Métricas y logs sin contraseñas, JWT, hashes ni datos personales innecesarios.
- Revisión de accesibilidad y usabilidad para la SPA; estas metas no se cumplen solo con backend.

## Trazabilidad de calidad
Cada prueba debe referenciar el ID de requisito no funcional aplicable, registrar resultado y guardar evidencia reproducible. Consultar el Plan original para las fórmulas exactas de medición.
