# 05. Arquitectura del backend

## Estilo aprobado
Arquitectura monolítica por capas, con frontend Angular y backend Spring Boot como aplicaciones separadas. No adoptar microservicios ni arquitectura hexagonal sin aprobación explícita.

## Paquetes
El paquete raíz real es `com.uniquindio.teen_sport` (generado por Spring Initializr; `teen-sport` no es un nombre de paquete válido). Se conserva en lugar de `com.teensport` para respetar la estructura ya creada.

```text
com.uniquindio.teen_sport
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
La base de datos relacional es el almacén principal: PostgreSQL 18.6. Por ahora no se usan migraciones versionadas; el esquema se deriva de las entidades JPA, que mapean el MER. Utilizar Spring Data JPA/ORM para operaciones ordinarias y SQL nativo selectivo cuando exista justificación por complejidad o rendimiento. La SQL debe quedar encapsulada en repositorios.

Configuración de la conexión (`application.properties`): `DB_URL` (por defecto `jdbc:postgresql://localhost:5432/teen_sport`), `DB_USERNAME` (por defecto `teen_sport`) y `DB_PASSWORD` (obligatoria, sin valor por defecto). `spring.jpa.hibernate.ddl-auto=update` crea y amplía tablas a partir de las entidades, sin eliminar columnas ni datos; `spring.jpa.open-in-view=false` obliga a resolver las cargas dentro de los servicios.

Variables locales en un archivo `.env`: `application.properties` importa `optional:file:.env[.properties]` desde el directorio de trabajo (la raíz del proyecto al usar `gradlew bootRun`). Para usarlo, copiar `.env.example` como `.env` y completar los valores. El archivo `.env` está en `.gitignore` y nunca se versiona; se lee como archivo de propiedades (`CLAVE=valor`, sin `export` ni comillas). Las variables de entorno del sistema tienen prioridad sobre el `.env`, y si el archivo no existe la aplicación usa solo las variables del sistema.

Variables de entorno de seguridad:

| Variable | Obligatoria | Uso |
|---|---|---|
| `JWT_SECRET` | Sí | Secreto HS256 de los JWT, mínimo 32 bytes. Sin él la aplicación no arranca. |
| `JWT_EXPIRATION_SECONDS` | No (3600) | Vigencia de los tokens. |
| `CORS_ALLOWED_ORIGINS` | No (`http://localhost:4200`) | Orígenes del frontend separados por comas; no admite comodines. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_ID_NUMBER` | No | Crean el administrador inicial al arrancar si no existe ningún ADMIN. Deben definirse las tres o ninguna. |
| `ADMIN_ID_TYPE`, `ADMIN_FIRST_NAMES`, `ADMIN_LAST_NAMES` | No (`CC`, `Administrador`, `TeenSport`) | Datos complementarios del administrador inicial. |

Al arrancar, `config/ReferenceDataInitializer` crea los roles `ADMIN`, `COACH` y `GUARDIAN` si faltan y, si se configuró, el administrador inicial. Es idempotente.

## Contrato API
La especificación OpenAPI define rutas bajo `/api/v1`, modelos JSON, roles, códigos de error y códigos HTTP. Mantener el archivo como contrato versionado y validar que implementación y pruebas concuerden con él.

El prefijo `/api/v1` se aplica en `config/WebConfig` a todos los controladores del paquete `controller`; cada controlador declara solo la ruta del recurso (por ejemplo `@RequestMapping("/athletes")`). Actuator queda fuera del prefijo (`/actuator/health`). Existe un único ambiente general (`application.properties`), sin archivos ni perfiles por ambiente; springdoc (`/v3/api-docs`, Swagger UI) está habilitado.

## Transacciones y consistencia
- Agrupar cambios relacionados en transacciones.
- La creación de matrícula debe reservar cupo de manera concurrentemente segura.
- La confirmación, rechazo, cancelación y cambio de grupo deben mantener coherentes estados y capacidad.
- El envío de correos que dependa de un commit debe ejecutarse después del commit; manejar fallos sin revertir indebidamente la operación ya confirmada.

## Observabilidad y configuración
- Mantener una única configuración general (`application.properties`) y los secretos fuera del repositorio, inyectados mediante variables de entorno.
- No registrar contraseñas, JWT, hashes ni datos personales innecesarios.
- Registrar errores con identificadores de correlación y contexto técnico mínimo.
- No exponer herramientas de depuración; la exposición de la documentación interactiva (Swagger UI) se revisará antes del despliegue.

## Despliegue previsto
El documento de arquitectura propone Jenkins y Azure. Confirmar servicios y variables antes de crear pipeline o infraestructura. No almacenar secretos de Azure/Brevo en `application.properties` versionado.
