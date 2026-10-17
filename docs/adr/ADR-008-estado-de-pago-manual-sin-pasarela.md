# ADR-008: Estado de pago manual sin pasarela en línea

- **Estado:** Aceptada.
- **Contexto:** El alcance académico no contempla procesamiento de pagos en línea.
- **Decisión:** El acudiente reporta un pago presencial; el administrador registra el pago y el backend actualiza el estado. No hay job automático para cambiar a `UP_TO_DATE`.
- **Alternativas:** Pasarela de pagos; conciliación automática.
- **Justificación:** Mantiene el alcance y el proceso operativo definido.
- **Consecuencias:** El estado depende de registro oportuno. `paymentReported=true` no significa pago confirmado; solo el registro administrativo lo confirma.
