package com.uniquindio.teen_sport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Catálogo de roles (tabla {@code roles} del MER).
 */
@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(name = "uk_roles_name", columnNames = "name"))
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 50)
    private RoleName name;

    @Column(name = "description", length = 255)
    private String description;

    protected Role() {
    }

    /**
     * @param name        nombre del rol
     * @param description descripción legible
     */
    public Role(RoleName name, String description) {
        this.name = name;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
