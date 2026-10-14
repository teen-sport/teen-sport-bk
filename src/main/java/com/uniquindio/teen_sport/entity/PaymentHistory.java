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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Pago presencial registrado por un administrador (tabla {@code payment_history} del MER).
 *
 * <p>El mes que cubre un pago es el de su {@code payment_date}; no hay una columna propia para ese
 * mes. La regla de un pago por mes y matrícula se valida en el servicio, bajo el bloqueo de la
 * matrícula, y no con una restricción única de base de datos.</p>
 */
@Entity
@Table(name = "payment_history")
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_enrollment"))
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by_admin_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_admin"))
    private User registeredBy;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "receipt_reference", length = 100)
    private String receiptReference;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    protected PaymentHistory() {
    }

    /**
     * @param enrollment   matrícula pagada
     * @param registeredBy administrador que registra el pago
     * @param amountPaid   monto en COP
     * @param paymentDate  fecha del pago
     * @param createdAt    fecha de creación
     */
    public PaymentHistory(Enrollment enrollment, User registeredBy, BigDecimal amountPaid, LocalDate paymentDate,
                          Instant createdAt) {
        this.enrollment = enrollment;
        this.registeredBy = registeredBy;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public User getRegisteredBy() {
        return registeredBy;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    /**
     * @return el mes que cubre el pago: el de su fecha
     */
    public YearMonth getPeriod() {
        return YearMonth.from(paymentDate);
    }

    public String getReceiptReference() {
        return receiptReference;
    }

    public void setReceiptReference(String receiptReference) {
        this.receiptReference = receiptReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
