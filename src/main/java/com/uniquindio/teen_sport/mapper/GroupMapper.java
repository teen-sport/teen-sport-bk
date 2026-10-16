package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.DisciplineFee;
import com.uniquindio.teen_sport.dto.GroupAvailability;
import com.uniquindio.teen_sport.dto.GroupResponse;
import com.uniquindio.teen_sport.dto.IdName;
import com.uniquindio.teen_sport.dto.PersonRef;
import com.uniquindio.teen_sport.dto.ScheduleResponse;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.entity.User;
import java.util.Comparator;
import java.util.List;

/**
 * Convierte grupos, franjas y disponibilidad al formato del contrato. Del entrenador solo expone su id y
 * su nombre. Son funciones puras, sin estado ni dependencias.
 */
public final class GroupMapper {

    private static final Comparator<Schedule> SCHEDULE_ORDER =
            Comparator.comparing(Schedule::getDayOfWeek).thenComparing(Schedule::getStartTime);

    private GroupMapper() {
    }

    /**
     * @param group     grupo con disciplina, entrenador y cuenta del entrenador cargados
     * @param schedules franjas del grupo
     * @return el grupo con sus franjas ordenadas por día y hora
     */
    public static GroupResponse toResponse(Group group, List<Schedule> schedules) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                new IdName(group.getDiscipline().getId(), group.getDiscipline().getName()),
                coach(group),
                group.getMaxCapacity(),
                group.getStatus(),
                toResponses(schedules));
    }

    /**
     * @param group     grupo con disciplina, entrenador y cuenta del entrenador cargados
     * @param schedules franjas del grupo
     * @param occupiedSpots  cupos ocupados en el periodo consultado
     * @param availableSpots cupos que aún se pueden reservar
     * @return la disponibilidad del grupo (E5)
     */
    public static GroupAvailability toAvailability(Group group, List<Schedule> schedules, int occupiedSpots,
                                                   int availableSpots) {
        return new GroupAvailability(
                group.getId(),
                group.getName(),
                new DisciplineFee(group.getDiscipline().getId(), group.getDiscipline().getName(),
                        group.getDiscipline().getMonthlyFee()),
                coach(group),
                toResponses(schedules),
                group.getMaxCapacity(),
                occupiedSpots,
                availableSpots);
    }

    /**
     * @param schedules franjas
     * @return las franjas en el formato del contrato, ordenadas por día y hora
     */
    public static List<ScheduleResponse> toResponses(List<Schedule> schedules) {
        return schedules.stream().sorted(SCHEDULE_ORDER).map(GroupMapper::toResponse).toList();
    }

    /**
     * @param schedule franja
     * @return la franja en el formato del contrato
     */
    public static ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(schedule.getId(), schedule.getDayOfWeek(), schedule.getStartTime(),
                schedule.getEndTime());
    }

    private static PersonRef coach(Group group) {
        User coachUser = group.getCoach().getUser();
        return new PersonRef(group.getCoach().getId(), coachUser.getFirstName() + " " + coachUser.getLastName());
    }
}
