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
 * Perfil de entrenador asociado 1:1 a un usuario con rol {@code COACH} (tabla {@code coaches}).
 *
 * <p>No tiene estado propio: el estado del entrenador es el de su usuario ({@code users.status}).</p>
 */
@Entity
@Table(name = "coaches", uniqueConstraints = @UniqueConstraint(name = "uk_coaches_user", columnNames = "user_id"))
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_coach_user"))
    private User user;

    @Column(name = "specialty", length = 100)
    private String specialty;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    protected Coach() {
    }

    /**
     * @param user            cuenta del entrenador
     * @param specialty       especialidad, opcional
     * @param yearsExperience años de experiencia, opcional
     */
    public Coach(User user, String specialty, Integer yearsExperience) {
        this.user = user;
        this.specialty = specialty;
        this.yearsExperience = yearsExperience;
    }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }
}
