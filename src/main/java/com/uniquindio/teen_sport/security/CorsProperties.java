package com.uniquindio.teen_sport.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Orígenes autorizados por CORS ({@code app.cors.allowed-origins}).
 *
 * @param allowedOrigins lista explícita de orígenes del frontend; no se admiten comodines
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    /**
     * @throws IllegalArgumentException si la lista está vacía o contiene un comodín
     */
    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("app.cors.allowed-origins no puede estar vacío");
        }
        if (allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("app.cors.allowed-origins no admite comodines");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
