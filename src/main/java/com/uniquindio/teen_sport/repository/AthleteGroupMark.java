package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.AttendanceStatus;
import java.time.LocalDate;

/**
 * Una marca de asistencia de un deportista en un grupo, con los datos que necesita el reporte de
 * ausencias consecutivas (R4).
 */
public interface AthleteGroupMark {

    /**
     * @return id del deportista
     */
    Integer getAthleteId();

    /**
     * @return nombres del deportista
     */
    String getFirstNames();

    /**
     * @return apellidos del deportista
     */
    String getLastNames();

    /**
     * @return id del grupo
     */
    Integer getGroupId();

    /**
     * @return nombre del grupo
     */
    String getGroupName();

    /**
     * @return marca
     */
    AttendanceStatus getStatus();

    /**
     * @return fecha de la sesión
     */
    LocalDate getSessionDate();
}
