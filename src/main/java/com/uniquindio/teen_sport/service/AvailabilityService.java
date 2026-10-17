package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.dto.GroupAvailability;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Schedule;
import com.uniquindio.teen_sport.mapper.GroupMapper;
import com.uniquindio.teen_sport.repository.GroupRepository;
import com.uniquindio.teen_sport.repository.GroupSpecifications;
import com.uniquindio.teen_sport.repository.ScheduleRepository;
import com.uniquindio.teen_sport.service.SpotCounter.Occupancy;
import com.uniquindio.teen_sport.validation.PeriodPolicy;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disponibilidad de cupos por periodo (E5). Es la única forma en que un acudiente consulta los
 * grupos.
 */
@Service
public class AvailabilityService {

    private final GroupRepository groupRepository;
    private final ScheduleRepository scheduleRepository;
    private final SpotCounter spotCounter;
    private final AcademyCalendar calendar;

    /**
     * @param groupRepository    grupos
     * @param scheduleRepository franjas
     * @param spotCounter        cálculo de cupos
     * @param calendar           mes actual de la academia
     */
    public AvailabilityService(GroupRepository groupRepository, ScheduleRepository scheduleRepository,
                               SpotCounter spotCounter, AcademyCalendar calendar) {
        this.groupRepository = groupRepository;
        this.scheduleRepository = scheduleRepository;
        this.spotCounter = spotCounter;
        this.calendar = calendar;
    }

    /**
     * E5: grupos activos de disciplinas activas con sus cupos en el periodo. Las franjas y los cupos de
     * todos los grupos se cargan en una consulta cada uno.
     *
     * @param period       periodo, desde el mes actual
     * @param disciplineId filtro por disciplina
     * @return los grupos ordenados por nombre
     * @throws com.uniquindio.teen_sport.exception.BusinessException 422 {@code PERIOD_IN_PAST}
     */
    @Transactional(readOnly = true)
    public List<GroupAvailability> list(YearMonth period, Integer disciplineId) {
        PeriodPolicy.requireNotPast(period, calendar.currentPeriod());
        List<Group> groups = groupRepository.findAll(GroupSpecifications.openForEnrollment(disciplineId),
                GroupService.ORDER);
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Integer> groupIds = groups.stream().map(Group::getId).toList();
        Map<Integer, List<Schedule>> schedules = scheduleRepository.findByGroupIds(groupIds).stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getGroup().getId()));
        Map<Integer, Occupancy> occupancy = spotCounter.occupancy(groupIds, period);
        return groups.stream()
                .map(group -> toAvailability(group, schedules.getOrDefault(group.getId(), List.of()),
                        occupancy.getOrDefault(group.getId(), Occupancy.EMPTY)))
                .toList();
    }

    private static GroupAvailability toAvailability(Group group, List<Schedule> schedules, Occupancy occupancy) {
        return GroupMapper.toAvailability(group, schedules, Math.toIntExact(occupancy.startedBy()),
                Math.toIntExact(occupancy.available(group.getMaxCapacity())));
    }
}
