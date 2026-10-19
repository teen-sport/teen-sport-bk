# ADR-007: JWT y autorización centralizada por rol y propiedad

- **Estado:** Aceptada.
- **Contexto:** Existen roles con capacidades distintas y datos personales de menores. El frontend está desacoplado.
- **Decisión:** Autenticar mediante JWT Bearer y aplicar autorización en el backend. Los roles contractuales son `ADMIN`, `COACH` y `GUARDIAN`; comprobar también pertenencia al recurso.
- **Alternativas:** Sesión tradicional; autorización solo en frontend; credenciales sin hash seguro.
- **Justificación:** La API necesita identidad portable y controles uniformes en todos los endpoints.
- **Consecuencias:** Validar firma y expiración, mantener secretos seguros, aplicar filtros de propiedad en listados y probar accesos indebidos. El token no sustituye la comprobación de que el deportista/grupo pertenece al actor.
- **Consecuencias adicionales (SAD):** definir políticas de expiración de tokens, almacenamiento seguro de contraseñas (hash) y un mecanismo seguro de recuperación de contraseña (pendiente; ver `06-seguridad-y-autorizacion.md`).
- **Implementación (2026-10-11):** emitir y validar el JWT con el soporte de resource server de Spring Security (Nimbus), sin librerías JWT de terceros; se incorporará en la Fase 1.
