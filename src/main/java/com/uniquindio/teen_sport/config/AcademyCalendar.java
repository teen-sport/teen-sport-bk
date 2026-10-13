package com.uniquindio.teen_sport.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fecha de negocio de la academia.
 *
 * <p>El reloj de la aplicación trabaja en UTC, pero las reglas que dependen del día (edad de un
 * deportista, periodos, sesiones) usan la fecha local de la academia. Sin esta conversión, desde
 * las 7 p. m. en Colombia la fecha UTC ya sería la del día siguiente. La zona se configura con
 * {@code APP_TIME_ZONE} (por defecto {@code America/Bogota}); una zona inválida impide arrancar.</p>
 */
@Component
public class AcademyCalendar {

    private final Clock clock;
    private final ZoneId zone;

    /**
     * @param clock    reloj de la aplicación
     * @param timeZone identificador de zona horaria IANA de la academia
     */
    public AcademyCalendar(Clock clock, @Value("${app.time-zone}") String timeZone) {
        this.clock = clock;
        this.zone = ZoneId.of(timeZone);
    }

    /**
     * @return la fecha actual en la zona de la academia
     */
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    /**
     * @return zona horaria de la academia
     */
    public ZoneId zone() {
        return zone;
    }
}
