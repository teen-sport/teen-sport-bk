package com.uniquindio.teen_sport.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación.
 *
 * <p>Los servicios, la emisión y validación de JWT y los errores obtienen la hora de este
 * bean en lugar de llamar a {@code Instant.now()}, de modo que las pruebas pueden fijar el
 * tiempo (por ejemplo, para comprobar el vencimiento de los tokens).</p>
 */
@Configuration
public class ClockConfig {

    /**
     * @return reloj del sistema en UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
