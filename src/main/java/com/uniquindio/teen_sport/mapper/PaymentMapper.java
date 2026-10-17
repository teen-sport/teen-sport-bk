package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.PaymentResponse;
import com.uniquindio.teen_sport.dto.PersonRef;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.PaymentHistory;
import com.uniquindio.teen_sport.entity.User;

/**
 * Convierte pagos al formato del contrato. Del deportista solo expone su id, nombres y apellidos, y
 * del administrador, el id de su cuenta y su nombre.
 */
public final class PaymentMapper {

    private PaymentMapper() {
    }

    /**
     * @param payment pago con la matrícula (deportista, grupo y disciplina) y el administrador cargados
     * @return el pago
     */
    public static PaymentResponse toResponse(PaymentHistory payment) {
        Enrollment enrollment = payment.getEnrollment();
        User admin = payment.getRegisteredBy();
        return new PaymentResponse(payment.getId(), enrollment.getId(), EnrollmentMapper.athlete(enrollment),
                EnrollmentMapper.discipline(enrollment), payment.getPeriod(), payment.getAmountPaid(),
                payment.getPaymentDate(), payment.getReceiptReference(), payment.getNotes(),
                new PersonRef(admin.getId(), admin.getFirstName() + " " + admin.getLastName()),
                payment.getCreatedAt());
    }
}
