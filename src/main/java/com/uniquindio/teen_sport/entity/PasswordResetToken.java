package com.uniquindio.teen_sport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Token de recuperación de contraseña (tabla {@code password_reset_tokens}, creada el 2026-11-10).
 *
 * <p>Solo se guarda el hash SHA-256 del token, nunca el token: quien lea la base no puede usarlo.
 * Un token sirve una sola vez ({@code usedAt}) y hasta {@code expiresAt}. Una solicitud nueva o un
 * restablecimiento adelantan {@code expiresAt} de los tokens abiertos de la cuenta, para invalidarlos sin
 * marcarlos como usados.</p>
 */
@Entity
@Table(name = "password_reset_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_tokens_hash", columnNames = "token_hash"),
        indexes = @Index(name = "idx_password_reset_tokens_user", columnList = "user_id"))
public class PasswordResetToken {

    /** Longitud del hash SHA-256 en hexadecimal. */
    public static final int HASH_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_password_reset_token_user"))
    private User user;

    @Column(name = "token_hash", nullable = false, length = HASH_LENGTH)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetToken() {
    }

    /**
     * @param user      cuenta que pidió la recuperación
     * @param tokenHash hash SHA-256 del token, en hexadecimal
     * @param expiresAt fin de la vigencia
     * @param createdAt momento de emisión
     */
    public PasswordResetToken(User user, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /**
     * @param now momento de la consulta
     * @return {@code true} si no se ha usado y sigue vigente
     */
    public boolean isUsableAt(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /**
     * @param now momento del restablecimiento
     */
    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
