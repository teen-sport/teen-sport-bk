package com.uniquindio.teen_sport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita {@code @Async} para enviar los correos fuera del hilo de la petición. Usa el ejecutor que
 * Spring Boot configura por defecto ({@code spring.task.execution.*}).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
