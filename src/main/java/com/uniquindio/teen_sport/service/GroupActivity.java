package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.SessionStatus;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.TrainingSessionRepository;
import org.springframework.stereotype.Component;

/**
 * Actividad de un grupo que condiciona su gestión (H9–H11): matrículas vigentes y sesiones futuras.
 *
 * <p>Una sesión es futura si está programada y su fecha es hoy o posterior, en la zona de la academia.
 * Quien llama debe tener bloqueado el grupo, para que no entren matrículas ni sesiones nuevas entre la
 * comprobación y el cambio.</p>
 */
@Component
public class GroupActivity {

    private final EnrollmentRepository enrollmentRepository;
    private final TrainingSessionRepository sessionRepository;
    private final AcademyCalendar calendar;

    /**
     * @param enrollmentRepository matrículas
     * @param sessionRepository    sesiones
     * @param calendar             fecha de la academia
     */
    public GroupActivity(EnrollmentRepository enrollmentRepository, TrainingSessionRepository sessionRepository,
                         AcademyCalendar calendar) {
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
        this.calendar = calendar;
    }

    /**
     * @param groupId id del grupo
     * @return matrículas vigentes ({@code PENDING_REVIEW} o {@code ACTIVE}) del grupo
     */
    public long inForceEnrollments(Integer groupId) {
        return enrollmentRepository.countByGroupIdAndStatusIn(groupId, EnrollmentStatus.IN_FORCE);
    }

    /**
     * @param groupId id del grupo
     * @return {@code true} si el grupo tiene matrículas vigentes o sesiones futuras
     */
    public boolean hasActivity(Integer groupId) {
        return enrollmentRepository.existsByGroupIdAndStatusIn(groupId, EnrollmentStatus.IN_FORCE)
                || sessionRepository.existsByGroupIdAndStatusAndSessionDateGreaterThanEqual(groupId,
                SessionStatus.SCHEDULED, calendar.today());
    }

    /**
     * Pasa al nuevo entrenador las sesiones futuras del grupo; las anteriores conservan al que las dictó.
     *
     * @param groupId id del grupo
     * @param coach   nuevo entrenador
     */
    public void moveFutureSessions(Integer groupId, Coach coach) {
        sessionRepository.reassignScheduledFrom(groupId, coach, calendar.today());
    }
}
