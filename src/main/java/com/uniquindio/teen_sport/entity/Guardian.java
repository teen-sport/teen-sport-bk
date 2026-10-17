package com.uniquindio.teen_sport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Perfil de acudiente asociado 1:1 a un usuario con rol {@code GUARDIAN} (tabla {@code guardians}).
 */
@Entity
@Table(name = "guardians", uniqueConstraints = @UniqueConstraint(name = "uk_guardians_user", columnNames = "user_id"))
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_guardian_user"))
    private User user;

    @Column(name = "address", length = 255)
    private String address;

    protected Guardian() {
    }

    /**
     * @param user    cuenta del acudiente
     * @param address dirección de residencia, opcional
     */
    public Guardian(User user, String address) {
        this.user = user;
        this.address = address;
    }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
