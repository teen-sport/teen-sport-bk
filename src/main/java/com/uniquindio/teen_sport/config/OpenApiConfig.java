package com.uniquindio.teen_sport.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la especificación OpenAPI generada a partir del código.
 *
 * <p>La fuente de verdad del contrato es el archivo {@code openapi.yaml} versionado en
 * {@code docs/}; la especificación generada sirve para comparar la implementación con
 * ese contrato. Swagger UI y {@code /v3/api-docs} están habilitados; su exposición por
 * ambiente se decidirá cuando se configuren los ambientes.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Nombre del esquema de seguridad Bearer JWT declarado en el contrato. */
    public static final String BEARER_SCHEME = "bearerAuth";

    /**
     * Construye la descripción general de la API y declara el esquema Bearer JWT.
     *
     * @return metadatos OpenAPI de TeenSport
     */
    @Bean
    public OpenAPI teenSportOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TeenSport API")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
