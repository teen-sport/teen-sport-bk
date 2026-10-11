# ADR-009: Integración de correo mediante adaptador Brevo

- **Estado:** Aceptada según la descripción de arquitectura.
- **Contexto:** El sistema requiere notificaciones y no debe acoplar la lógica de negocio directamente al proveedor.
- **Decisión:** Usar SDK Java de Brevo detrás de un componente adaptador dentro de `integration/`.
- **Alternativas:** Acoplar servicios de negocio directamente al SDK; proveedor sin abstracción.
- **Justificación:** Facilita pruebas, reemplazo del proveedor y tratamiento centralizado de errores.
- **Consecuencias:** Gestionar credenciales como secretos, limitar los datos personales enviados, registrar resultados de envío sin información sensible y definir política de reintentos/errores. Cuando el envío dependa de una transacción, enviarlo después del commit.
