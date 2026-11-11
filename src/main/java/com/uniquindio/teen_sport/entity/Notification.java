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
 * Registro de un correo enviado o intentado a un usuario (tabla {@code notifications} del MER).
 *
 * <p>Nace {@code PENDING} con cero intentos y termina {@code SENT} o {@code FAILED}. No guarda el
 * cuerpo del correo ni la dirección: solo el tipo y el asunto, para medir la entrega (RNF-14) sin
 * duplicar datos personales.</p>
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 50)
    private DeliveryStatus deliveryStatus;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Notification() {
    }

    /**
     * Notificación pendiente, sin intentos.
     *
     * @param user      destinatario
     * @param type      evento
     * @param subject   asunto del correo
     * @param createdAt momento de registro
     */
    public Notification(User user, NotificationType type, String subject, Instant createdAt) {
        this.user = user;
        this.type = type;
        this.subject = subject;
        this.createdAt = createdAt;
        this.deliveryStatus = DeliveryStatus.PENDING;
    }

    /**
     * @param status   estado tras el intento
     * @param attempts intentos realizados hasta ahora
     */
    public void recordAttempt(DeliveryStatus status, int attempts) {
        this.deliveryStatus = status;
        this.attempts = attempts;
    }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public NotificationType getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
