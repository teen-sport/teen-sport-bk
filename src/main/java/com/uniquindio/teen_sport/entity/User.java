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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Cuenta de acceso y datos comunes de cualquier usuario (tabla {@code users} del MER).
 *
 * <p>El correo y el número de documento son únicos. El estado {@link EntityStatus#INACTIVE}
 * impide iniciar sesión. La contraseña solo se guarda como hash BCrypt.</p>
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_document_number", columnNames = "document_number")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role"))
    private Role role;

    @Column(name = "document_type", nullable = false, length = 10)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "accepted_data_policy", nullable = false)
    private boolean acceptedDataPolicy;

    @Column(name = "accepted_data_policy_at")
    private Instant acceptedDataPolicyAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    protected User() {
    }

    /**
     * Crea una cuenta activa sin aceptación de la política de datos.
     *
     * @param role         rol del usuario
     * @param email        correo normalizado
     * @param passwordHash hash BCrypt de la contraseña
     * @param createdAt    momento de creación
     */
    public User(Role role, String email, String passwordHash, Instant createdAt) {
        this.role = role;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.status = EntityStatus.ACTIVE;
    }

    /**
     * Registra la aceptación de la política de tratamiento de datos (Ley 1581 de 2012).
     *
     * @param acceptedAt momento de la aceptación
     */
    public void acceptDataPolicy(Instant acceptedAt) {
        this.acceptedDataPolicy = true;
        this.acceptedDataPolicyAt = acceptedAt;
    }

    /**
     * @return {@code true} si la cuenta puede iniciar sesión
     */
    public boolean isActive() {
        return status == EntityStatus.ACTIVE;
    }

    public Integer getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    /**
     * @param email nuevo correo, ya normalizado y verificado como disponible
     */
    public void changeEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Reemplaza la contraseña. Recibe siempre el hash, nunca la contraseña en claro.
     *
     * @param passwordHash nuevo hash BCrypt
     */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isAcceptedDataPolicy() {
        return acceptedDataPolicy;
    }

    public Instant getAcceptedDataPolicyAt() {
        return acceptedDataPolicyAt;
    }

    public EntityStatus getStatus() {
        return status;
    }

    public void setStatus(EntityStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
