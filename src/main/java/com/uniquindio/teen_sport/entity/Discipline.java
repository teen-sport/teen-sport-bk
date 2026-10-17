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
import java.math.BigDecimal;

/**
 * Disciplina deportiva del catálogo (tabla {@code disciplines} del MER).
 *
 * <p>El nombre es único. La mensualidad ({@code DECIMAL(10,2)}, en COP) es la que se cobra a las
 * matrículas; un cambio aplica a los pagos registrados desde ese momento.</p>
 */
@Entity
@Table(name = "disciplines", uniqueConstraints = @UniqueConstraint(name = "uk_disciplines_name", columnNames = "name"))
public class Discipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status;

    protected Discipline() {
    }

    /**
     * Crea una disciplina activa.
     *
     * @param name        nombre
     * @param description descripción, opcional
     * @param monthlyFee  mensualidad en COP
     */
    public Discipline(String name, String description, BigDecimal monthlyFee) {
        this.name = name;
        this.description = description;
        this.monthlyFee = monthlyFee;
        this.status = EntityStatus.ACTIVE;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(BigDecimal monthlyFee) {
        this.monthlyFee = monthlyFee;
    }

    public EntityStatus getStatus() {
        return status;
    }

    public void setStatus(EntityStatus status) {
        this.status = status;
    }
}
