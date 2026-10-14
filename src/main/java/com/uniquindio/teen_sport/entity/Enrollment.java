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
import java.time.YearMonth;

/**
 * Matrícula de un deportista en un grupo (tabla {@code enrollments} del MER).
 *
 * <p>{@code period_month} es el mes de inicio, guardado como texto {@code YYYY-MM}: con ese formato
 * el orden alfabético coincide con el cronológico, y las consultas comparan el texto directamente.
 * La matrícula sigue vigente mes a mes desde ese mes hasta que se rechaza o se cancela.</p>
 *
 * <p>No se mapean {@code medical_certificate_path} ni {@code guardian_authorization_path}: el sistema
 * no gestiona soportes documentales y esas columnas admiten nulos.</p>
 */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false, foreignKey = @ForeignKey(name = "fk_enrollment_athlete"))
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_enrollment_group"))
    private Group group;

    @Column(name = "period_month", nullable = false, length = 20)
    private String periodMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "payment_reported", nullable = false)
    private boolean paymentReported;

    @Column(name = "payment_reported_at")
    private Instant paymentReportedAt;

    @Column(name = "payment_report_note", length = 255)
    private String paymentReportNote;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Enrollment() {
    }

    /**
     * Crea una matrícula pendiente de revisión, con el pago en mora y sin pago reportado.
     *
     * @param athlete   deportista
     * @param group     grupo
     * @param period    mes de inicio
     * @param createdAt fecha de creación
     */
    public Enrollment(Athlete athlete, Group group, YearMonth period, Instant createdAt) {
        this.athlete = athlete;
        this.group = group;
        this.periodMonth = period.toString();
        this.status = EnrollmentStatus.PENDING_REVIEW;
        this.paymentStatus = PaymentStatus.OVERDUE;
        this.paymentReported = false;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public YearMonth getPeriod() {
        return YearMonth.parse(periodMonth);
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public boolean isPaymentReported() {
        return paymentReported;
    }

    public void setPaymentReported(boolean paymentReported) {
        this.paymentReported = paymentReported;
    }

    public Instant getPaymentReportedAt() {
        return paymentReportedAt;
    }

    public void setPaymentReportedAt(Instant paymentReportedAt) {
        this.paymentReportedAt = paymentReportedAt;
    }

    public String getPaymentReportNote() {
        return paymentReportNote;
    }

    public void setPaymentReportNote(String paymentReportNote) {
        this.paymentReportNote = paymentReportNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
