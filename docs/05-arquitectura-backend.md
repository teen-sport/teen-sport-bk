# 05. Arquitectura del backend

## Estilo aprobado
Arquitectura monolítica por capas, con frontend Angular y backend Spring Boot como aplicaciones separadas. No adoptar microservicios ni arquitectura hexagonal sin aprobación explícita.

## Paquetes propuestos
```text
com.teensport
├── config/          # configuración Spring, CORS, OpenAPI y beans
├── security/        # filtro JWT, utilidades de token, principal autenticado
├── controller/      # adaptadores HTTP y endpoints REST
├── service/         # casos de uso y reglas de negocio
├── repository/      # Spring Data JPA y consultas de persistencia
├── entity/          # entidades JPA
├── dto/             # solicitudes y respuestas API
├── mapper/          # conversiones DTO-entidad cuando sean necesarias
├── validation/      # validaciones de dominio reutilizables
├── exception/       # excepciones y manejador global
└── integration/     # adaptadores para Brevo y servicios externos
```

Los nombres son una propuesta de paquetes; respetar la estructura ya creada si existe y no mover archivos sin una necesidad clara.

## Responsabilidades
- Controller: interpretar HTTP, validar el DTO de entrada, invocar un servicio y mapear el resultado a HTTP.
- Service: ejecutar el caso de uso, autorizar acceso a los recursos en coordinación con el contexto de seguridad, aplicar reglas de negocio y definir límites transaccionales.
- Repository: encapsular persistencia; no debe contener lógica de presentación ni decisiones de autorización por rol general.
- Entity: representar el modelo persistente y sus relaciones, sin convertirse en contrato público.
- DTO: delimitar los campos que acepta y devuelve la API; prevenir asignación masiva.
- Mapper: mantener conversiones explícitas y probables.
- Security: autenticar JWT y aplicar permisos; las comprobaciones de pertenencia al recurso también deben hacerse en el caso de uso.
- Exception: traducir fallos esperados a respuestas HTTP estables.
- Integration: aislar llamadas a Brevo para facilitar pruebas y reemplazo del proveedor.

## Persistencia
La base de datos relacional es el almacén principal. Utilizar Spring Data JPA/ORM para operaciones ordinarias y SQL nativo selectivo cuando exista justificación por complejidad o rendimiento. La SQL debe quedar encapsulada en repositorios.

## Contrato API
La especificación OpenAPI define rutas bajo `/api/v1`, modelos JSON, roles, códigos de error y códigos HTTP. Mantener el archivo como contrato versionado y validar que implementación y pruebas concuerden con él.

## Transacciones y consistencia
- Agrupar cambios relacionados en transacciones.
- La creación de matrícula debe reservar cupo de manera concurrentemente segura.
- La confirmación, rechazo, cancelación y cambio de grupo deben mantener coherentes estados y capacidad.
- El envío de correos que dependa de un commit debe ejecutarse después del commit; manejar fallos sin revertir indebidamente la operación ya confirmada.

## Observabilidad y configuración
- Separar configuración por entorno y mantener secretos fuera del repositorio.
- No registrar contraseñas, JWT, hashes ni datos personales innecesarios.
- Registrar errores con identificadores de correlación y contexto técnico mínimo.
- No exponer herramientas de depuración o documentación interactiva públicamente sin decisión por entorno.

## Despliegue previsto
El documento de arquitectura propone Jenkins y Azure. Confirmar servicios, variables y ambientes reales antes de crear pipeline o infraestructura. No almacenar secretos de Azure/Brevo en `application.yml` versionado.
