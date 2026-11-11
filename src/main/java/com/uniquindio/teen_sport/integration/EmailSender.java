package com.uniquindio.teen_sport.integration;

/**
 * Envío de correos a través de un proveedor externo (ADR-09). Los servicios de negocio dependen de
 * esta interfaz y no del proveedor, que puede sustituirse sin cambiarlos.
 */
public interface EmailSender {

    /**
     * Entrega el correo al proveedor. Volver sin excepción significa que el proveedor lo aceptó, no que
     * haya llegado al buzón.
     *
     * @param message correo
     * @throws EmailDeliveryException si el proveedor no aceptó el correo
     */
    void send(EmailMessage message);
}
