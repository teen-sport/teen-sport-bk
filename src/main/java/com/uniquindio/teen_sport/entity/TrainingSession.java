package com.uniquindio.teen_sport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Sesión de entrenamiento de un grupo (tabla {@code training_sessions} del MER).
 *
 * <p>La sesión guarda el entrenador que la dicta: al crearla es el del grupo y, si el grupo cambia de
 * entrenador (H11), solo las sesiones futuras pasan al nuevo. Un grupo no puede tener dos sesiones con
 * la misma fecha y hora de inicio; el MER no define una restricción única, así que la regla se valida
 * en el servicio bajo el bloqueo del grupo.</p>
 */
@Entity
@Table(name = "training_sessions")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_session_group"))
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false, foreignKey = @ForeignKey(name = "fk_session_coach"))
    private Coach coach;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    protected TrainingSession() {
    }

    /**
     * Crea una sesión programada a cargo del entrenador actual del grupo.
     *
     * @param group     grupo
     * @param date      fecha
     * @param startTime hora de inicio
     * @param endTime   hora de fin
     */
    public TrainingSession(Group group, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.group = group;
        this.coach = group.getCoach();
        this.sessionDate = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = SessionStatus.SCHEDULED;
    }

    public Integer getId() {
        return id;
    }

    public Group getGroup() {
        return group;
    }

    public Coach getCoach() {
        return coach;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * @param date      nueva fecha
     * @param startTime nueva hora de inicio
     * @param endTime   nueva hora de fin
     */
    public void reschedule(LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.sessionDate = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
