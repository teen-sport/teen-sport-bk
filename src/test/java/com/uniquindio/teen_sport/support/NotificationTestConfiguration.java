package com.uniquindio.teen_sport.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Correo de las pruebas de integración.
 *
 * <ul>
 *   <li>{@link RecordingEmailSender} reemplaza a Brevo: ninguna prueba envía correos reales, aunque la
 *       máquina tenga {@code BREVO_API_KEY} configurada.</li>
 *   <li>El ejecutor de {@code @Async} es síncrono, para que la notificación ya esté registrada cuando
 *       la petición HTTP de la prueba responde. En producción el envío sigue siendo asíncrono.</li>
 * </ul>
 */
@TestConfiguration(proxyBeanMethods = false)
public class NotificationTestConfiguration {

    /**
     * @return proveedor de correo en memoria
     */
    @Bean
    @Primary
    RecordingEmailSender recordingEmailSender() {
        return new RecordingEmailSender();
    }

    /**
     * Nombre {@code taskExecutor}: es el que {@code @EnableAsync} usa por defecto.
     *
     * @return ejecutor que corre las tareas en el hilo que las pide
     */
    @Bean
    TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
