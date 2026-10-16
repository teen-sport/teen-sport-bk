package com.uniquindio.teen_sport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;

/**
 * Deportista menor de edad a cargo de un acudiente (tabla {@code athletes} del MER).
 *
 * <p>Contiene datos personales de un menor: solo se expone a su acudiente y al administrador.
 * El número de documento es único. No se elimina: la salida de la academia se gestiona con la
 * matrícula.</p>
 */
@Entity
@Table(name = "athletes",
        uniqueConstraints = @UniqueConstraint(name = "uk_athletes_document_number", columnNames = "document_number"))
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false, foreignKey = @ForeignKey(name = "fk_athlete_guardian"))
    private Guardian guardian;

    @Column(name = "document_type", nullable = false, length = 10)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Athlete() {
    }

    /**
     * @param guardian  acudiente responsable
     * @param createdAt momento del registro
     */
    public Athlete(Guardian guardian, Instant createdAt) {
        this.guardian = guardian;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public Guardian getGuardian() {
        return guardian;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
