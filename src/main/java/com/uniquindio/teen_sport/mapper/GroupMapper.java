package com.uniquindio.teen_sport.mapper;

import com.uniquindio.teen_sport.dto.GroupResponse;
import com.uniquindio.teen_sport.dto.IdName;
import com.uniquindio.teen_sport.dto.PersonRef;
import com.uniquindio.teen_sport.dto.ScheduleResponse;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.entity.User;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte grupos y franjas al formato del contrato. Del entrenador solo expone su id y su nombre.
 */
@Component
public class GroupMapper {

    private static final Comparator<Schedule> SCHEDULE_ORDER =
            Comparator.comparing(Schedule::getDayOfWeek).thenComparing(Schedule::getStartTime);

    /**
     * @param group     grupo con disciplina, entrenador y cuenta del entrenador cargados
     * @param schedules franjas del grupo
     * @return el grupo con sus franjas ordenadas por día y hora
     */
    public GroupResponse toResponse(Group group, List<Schedule> schedules) {
        User coachUser = group.getCoach().getUser();
        return new GroupResponse(
                group.getId(),
                group.getName(),
                new IdName(group.getDiscipline().getId(), group.getDiscipline().getName()),
                new PersonRef(group.getCoach().getId(), coachUser.getFirstName() + " " + coachUser.getLastName()),
                group.getMaxCapacity(),
                group.getStatus(),
                schedules.stream().sorted(SCHEDULE_ORDER).map(GroupMapper::toResponse).toList());
    }

    /**
     * @param schedule franja
     * @return la franja en el formato del contrato
     */
    public static ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(schedule.getId(), schedule.getDayOfWeek(), schedule.getStartTime(),
                schedule.getEndTime());
    }
}
