package com.uniquindio.teen_sport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración MVC transversal de la API.
 *
 * <p>Aplica el prefijo versionado del contrato ({@value #API_PREFIX}) a todos los
 * controladores del paquete {@code controller}. Así cada controlador declara solo la
 * ruta del recurso (por ejemplo {@code /athletes}), igual que el OpenAPI, y los
 * endpoints de Actuator conservan su ruta propia para las sondas del contenedor.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Prefijo común de todas las rutas REST declaradas en el contrato. */
    public static final String API_PREFIX = "/api/v1";

    private static final String CONTROLLER_PACKAGE = "com.uniquindio.teen_sport.controller";

    /**
     * Registra el prefijo {@value #API_PREFIX} para los controladores del paquete {@code controller}.
     *
     * @param configurer configurador de coincidencia de rutas de Spring MVC
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, HandlerTypePredicate.forBasePackage(CONTROLLER_PACKAGE));
    }
}
