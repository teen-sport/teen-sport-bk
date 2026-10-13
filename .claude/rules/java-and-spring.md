# Reglas de Java y Spring Boot

- Seguir las convenciones de Java y las prácticas recomendadas de Spring Boot.
- Respetar la versión de Java y Spring Boot configurada en el proyecto.
- Usar inyección de dependencias por constructor.
- Evitar inyección de dependencias mediante campos.
- Mantener los métodos cortos, legibles y enfocados.
- Preferir tipos específicos en lugar de tipos genéricos innecesarios.
- Usar `Optional` principalmente como tipo de retorno cuando aporte claridad, no como sustituto general de cualquier valor.
- Utilizar Bean Validation para validar DTO y entradas HTTP.
- Aplicar `@Transactional` en el límite apropiado de las operaciones de negocio que requieren atomicidad.
- No utilizar `@Transactional` indiscriminadamente en controllers ni repositories.
- Evitar lógica de negocio en callbacks, entidades o métodos auxiliares sin una responsabilidad clara.
- Evitar constantes mágicas y duplicación de código.
- Mantener las convenciones de nombres de Java.
- No agregar dependencias sin justificar su necesidad.
- Documentar decisiones complejas y métodos públicos relevantes mediante Javadoc.
- No escribir comentarios que repitan literalmente lo que hace cada línea.