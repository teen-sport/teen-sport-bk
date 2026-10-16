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
import java.time.Instant;

/**
 * Marca de asistencia de un deportista en una sesión (tabla {@code attendance_records} del MER).
 *
 * <p>Hay como máximo una marca por sesión y deportista. El MER no define una restricción única, así que
 * la regla se garantiza en el servicio bajo el bloqueo de la sesión: guardar de nuevo la asistencia
 * (A8) actualiza la marca existente. {@code created_at} es el primer registro y {@code recorded_at} el
 * último.</p>
 */
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_session"))
    private TrainingSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_athlete"))
    private Athlete athlete;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    protected AttendanceRecord() {
    }

    /**
     * @param session    sesión
     * @param athlete    deportista
     * @param status     marca
     * @param recordedAt momento del registro
     */
    public AttendanceRecord(TrainingSession session, Athlete athlete, AttendanceStatus status, Instant recordedAt) {
        this.session = session;
        this.athlete = athlete;
        this.status = status;
        this.createdAt = recordedAt;
        this.recordedAt = recordedAt;
    }

    public Integer getId() {
        return id;
    }

    public TrainingSession getSession() {
        return session;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    /**
     * Reemplaza la marca y actualiza el momento del registro; conserva el del primero.
     *
     * @param status     nueva marca
     * @param recordedAt momento del registro
     */
    public void mark(AttendanceStatus status, Instant recordedAt) {
        this.status = status;
        this.recordedAt = recordedAt;
    }
}
