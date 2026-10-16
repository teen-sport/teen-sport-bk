package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.PaymentHistory;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso al historial de pagos.
 */
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Integer>,
        JpaSpecificationExecutor<PaymentHistory> {

    /**
     * Pagos filtrados con la matrícula, el deportista, la disciplina y el administrador cargados en la
     * misma consulta.
     *
     * @param specification filtros
     * @param pageable      página y orden
     * @return la página de pagos
     */
    @Override
    @EntityGraph(attributePaths = {"enrollment", "enrollment.athlete", "enrollment.group",
            "enrollment.group.discipline", "registeredBy"})
    Page<PaymentHistory> findAll(Specification<PaymentHistory> specification, Pageable pageable);

    /**
     * @param enrollmentId id de la matrícula
     * @param from         primer día del mes
     * @param to           último día del mes
     * @return si la matrícula ya tiene un pago con fecha en ese rango
     */
    boolean existsByEnrollmentIdAndPaymentDateBetween(Integer enrollmentId, LocalDate from, LocalDate to);
}
