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

/**
 * Grupo de entrenamiento de una disciplina, a cargo de un entrenador (tabla {@code groups} del MER).
 *
 * <p>El nombre físico es {@code groups}, el del MER y el de la base desplegada (decisión del
 * 2026-10-13), aunque el contrato mencionaba {@code training_groups}. La capacidad máxima va de 1 a
 * 15 deportistas.</p>
 */
@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipline_id", nullable = false, foreignKey = @ForeignKey(name = "fk_group_discipline"))
    private Discipline discipline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false, foreignKey = @ForeignKey(name = "fk_group_coach"))
    private Coach coach;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status;

    protected Group() {
    }

    /**
     * Crea un grupo activo y sin horarios.
     *
     * @param discipline  disciplina
     * @param coach       entrenador a cargo
     * @param name        nombre
     * @param maxCapacity capacidad máxima
     */
    public Group(Discipline discipline, Coach coach, String name, Integer maxCapacity) {
        this.discipline = discipline;
        this.coach = coach;
        this.name = name;
        this.maxCapacity = maxCapacity;
        this.status = EntityStatus.ACTIVE;
    }

    public Integer getId() {
        return id;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public Coach getCoach() {
        return coach;
    }

    public void setCoach(Coach coach) {
        this.coach = coach;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public EntityStatus getStatus() {
        return status;
    }

    public void setStatus(EntityStatus status) {
        this.status = status;
    }
}
