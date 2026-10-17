package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.EnrollmentRepository;
import com.uniquindio.teen_sport.repository.GroupOccupancy;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Cálculo de cupos de los grupos.
 *
 * <p>Una matrícula {@code PENDING_REVIEW} o {@code ACTIVE} ocupa cupo desde su mes de inicio y en
 * todos los meses siguientes. Por eso el mes más cargado a partir de cualquier periodo es el que
 * cuenta todas las matrículas vigentes del grupo, y es contra ese total que se reserva un cupo: si
 * solo se contaran las que empezaron en el periodo pedido o antes, una matrícula que empieza más
 * tarde podría quedar sin cupo cuando llegue su mes (decisión del 2026-10-13).</p>
 *
 * <p>Quien reserva o reduce cupos debe tener bloqueado el grupo antes de contar.</p>
 */
@Component
public class SpotCounter {

    static final String NO_SPOTS_MESSAGE = "El grupo no tiene cupos disponibles para el periodo seleccionado.";

    private final EnrollmentRepository enrollmentRepository;

    /**
     * @param enrollmentRepository matrículas
     */
    public SpotCounter(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * @param groupId id del grupo
     * @return matrículas vigentes del grupo, empiecen cuando empiecen
     */
    public long inForce(Integer groupId) {
        return enrollmentRepository.countByGroupIdAndStatusIn(groupId, EnrollmentStatus.IN_FORCE);
    }

    /**
     * @param group grupo bloqueado
     * @throws BusinessException 409 {@code NO_AVAILABLE_SPOTS} si las matrículas vigentes ya llenan la capacidad
     */
    public void requireFreeSpot(Group group) {
        if (inForce(group.getId()) >= group.getMaxCapacity()) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_SPOTS, HttpStatus.CONFLICT, NO_SPOTS_MESSAGE);
        }
    }

    /**
     * Ocupación de varios grupos en un periodo, en una sola consulta.
     *
     * @param groupIds ids de grupos
     * @param period   periodo consultado
     * @return la ocupación por id de grupo; los grupos sin matrículas vigentes no aparecen
     */
    public Map<Integer, Occupancy> occupancy(Collection<Integer> groupIds, YearMonth period) {
        return enrollmentRepository.countOccupancy(groupIds, period.toString(), EnrollmentStatus.IN_FORCE).stream()
                .collect(Collectors.toMap(GroupOccupancy::getGroupId, Occupancy::of, (first, second) -> first));
    }

    /**
     * Cupos de un grupo en un periodo.
     *
     * @param startedBy matrículas vigentes que empezaron en el periodo o antes ({@code occupiedSpots} de E5)
     * @param inForce   todas las matrículas vigentes, que limitan los cupos que aún se pueden reservar
     */
    public record Occupancy(long startedBy, long inForce) {

        /** Grupo sin matrículas vigentes. */
        public static final Occupancy EMPTY = new Occupancy(0, 0);

        private static Occupancy of(GroupOccupancy row) {
            return new Occupancy(row.getStartedBy(), row.getInForce());
        }

        /**
         * @param maxCapacity capacidad del grupo
         * @return cupos que aún se pueden reservar a partir del periodo, nunca negativos
         */
        public long available(int maxCapacity) {
            return Math.max(0, maxCapacity - inForce);
        }
    }
}
