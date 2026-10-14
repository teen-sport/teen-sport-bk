package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.AthleteRef;
import com.uniquindio.teen_sport.dto.EnrollmentDetail;
import com.uniquindio.teen_sport.dto.EnrollmentSummary;
import com.uniquindio.teen_sport.dto.IdName;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import java.util.List;

/**
 * Convierte matrículas al formato del contrato. Del deportista solo expone su id, nombres y apellidos.
 * Son funciones puras, sin estado ni dependencias.
 */
public final class EnrollmentMapper {

    private EnrollmentMapper() {
    }

    /**
     * @param enrollment matrícula con deportista, grupo y disciplina cargados
     * @return la matrícula para los listados
     */
    public static EnrollmentSummary toSummary(Enrollment enrollment) {
        return new EnrollmentSummary(enrollment.getId(), athlete(enrollment), group(enrollment),
                discipline(enrollment), enrollment.getPeriod(), enrollment.getStatus(), enrollment.getPaymentStatus(),
                enrollment.isPaymentReported(), enrollment.getCreatedAt());
    }

    /**
     * @param enrollment matrícula con deportista, grupo y disciplina cargados
     * @param schedules  franjas del grupo
     * @return el detalle de la matrícula
     */
    public static EnrollmentDetail toDetail(Enrollment enrollment, List<Schedule> schedules) {
        return new EnrollmentDetail(enrollment.getId(), athlete(enrollment), group(enrollment),
                discipline(enrollment), enrollment.getPeriod(), enrollment.getStatus(), enrollment.getPaymentStatus(),
                enrollment.isPaymentReported(), enrollment.getCreatedAt(), GroupMapper.toResponses(schedules),
                enrollment.getReviewNotes(), enrollment.getConfirmedAt());
    }

    static AthleteRef athlete(Enrollment enrollment) {
        Athlete athlete = enrollment.getAthlete();
        return new AthleteRef(athlete.getId(), athlete.getFirstName(), athlete.getLastName());
    }

    private static IdName group(Enrollment enrollment) {
        Group group = enrollment.getGroup();
        return new IdName(group.getId(), group.getName());
    }

    static IdName discipline(Enrollment enrollment) {
        Discipline discipline = enrollment.getGroup().getDiscipline();
        return new IdName(discipline.getId(), discipline.getName());
    }
}
