# ADR-009: Integración de correo mediante adaptador Brevo

- **Estado:** Aceptada según la descripción de arquitectura.
- **Contexto:** El sistema requiere notificaciones (registro de cuenta, recuperación de contraseña, inscripciones, horarios y novedades) y no debe acoplar la lógica de negocio directamente al proveedor.
- **Alcance (actualizado el 2026-11-09):** la integración con Brevo se hace al final de la Entrega 3 (ver `docs/10-plan-implementacion.md`); mientras tanto no se simulan envíos.
- **Decisión:** Usar Brevo detrás de un componente adaptador dentro de `integration/`. ~~Mediante el SDK Java de Brevo.~~ **Modificada el 2026-11-10 (Entrega 3, etapa 6):** el adaptador `BrevoEmailSender` llama a la API REST v3 (`POST /v3/smtp/email`) con `RestClient` de Spring, detrás de la interfaz `EmailSender`.
- **Justificación del cambio:** solo se usa una operación de la API; `RestClient` ya viene con `spring-boot-starter-webmvc`, por lo que no se añade ninguna dependencia, y se evitan las librerías antiguas que arrastra el SDK (cliente HTTP y serializador propios), que generan alertas de seguridad. El desacoplamiento que pide este ADR no cambia: los servicios dependen de `EmailSender`.
- **Alternativas:** Acoplar servicios de negocio directamente al SDK; proveedor sin abstracción.
- **Justificación:** Facilita pruebas, reemplazo del proveedor y tratamiento centralizado de errores.
- **Consecuencias:** Gestionar credenciales como secretos, limitar los datos personales enviados, registrar resultados de envío sin información sensible y definir política de reintentos/errores. Cuando el envío dependa de una transacción, enviarlo después del commit.
- **Implementación (2026-11-10):** clave en `BREVO_API_KEY` y remitente en `MAIL_SENDER_EMAIL` (Railway y `.env` local, nunca en el repositorio); tiempos de espera de 5 s (conexión) y 10 s (respuesta); envío asíncrono después del commit, hasta 3 intentos para fallos transitorios (red, `429`, `5xx`) y registro de cada intento en `notifications`. Las pruebas usan un proveedor en memoria o un servidor HTTP simulado, nunca Brevo real.
