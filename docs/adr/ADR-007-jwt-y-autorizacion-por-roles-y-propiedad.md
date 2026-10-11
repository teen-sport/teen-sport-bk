# ADR-007: JWT y autorización centralizada por rol y propiedad

- **Estado:** Aceptada.
- **Contexto:** Existen roles con capacidades distintas y datos personales de menores. El frontend está desacoplado.
- **Decisión:** Autenticar mediante JWT Bearer y aplicar autorización en el backend. Los roles contractuales son `ADMIN`, `COACH` y `GUARDIAN`; comprobar también pertenencia al recurso.
- **Alternativas:** Sesión tradicional; autorización solo en frontend; credenciales sin hash seguro.
- **Justificación:** La API necesita identidad portable y controles uniformes en todos los endpoints.
- **Consecuencias:** Validar firma y expiración, mantener secretos seguros, aplicar filtros de propiedad en listados y probar accesos indebidos. El token no sustituye la comprobación de que el deportista/grupo pertenece al actor.
