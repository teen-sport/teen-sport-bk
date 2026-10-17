package com.uniquindio.teen_sport.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de datos PostgreSQL real para las pruebas que levantan el contexto completo.
 *
 * <p>Usa la misma versión del motor que el proyecto (18.6). {@link ServiceConnection}
 * reemplaza la conexión de {@code application.properties}, así que las pruebas no dependen
 * de variables de entorno ni de una base de datos local. Requiere Docker en ejecución.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfiguration {

    /** Imagen de PostgreSQL alineada con la versión aprobada del motor. */
    static final String POSTGRES_IMAGE = "postgres:18.6";

    /**
     * Contenedor compartido por el contexto de pruebas.
     *
     * @return contenedor PostgreSQL 18.6
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
