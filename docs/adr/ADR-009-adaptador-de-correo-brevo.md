# ADR-009: Integración de correo mediante adaptador Brevo

- **Estado:** Aceptada según la descripción de arquitectura.
- **Contexto:** El sistema requiere notificaciones (registro de cuenta, recuperación de contraseña, inscripciones, horarios y novedades) y no debe acoplar la lógica de negocio directamente al proveedor.
- **Alcance (actualizado el 2026-11-09):** la integración con Brevo se hace al final de la Entrega 3 (ver `docs/10-plan-implementacion.md`); mientras tanto no se simulan envíos.
- **Decisión:** Usar SDK Java de Brevo detrás de un componente adaptador dentro de `integration/`.
- **Alternativas:** Acoplar servicios de negocio directamente al SDK; proveedor sin abstracción.
- **Justificación:** Facilita pruebas, reemplazo del proveedor y tratamiento centralizado de errores.
- **Consecuencias:** Gestionar credenciales como secretos, limitar los datos personales enviados, registrar resultados de envío sin información sensible y definir política de reintentos/errores. Cuando el envío dependa de una transacción, enviarlo después del commit.
