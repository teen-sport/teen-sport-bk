package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.AthleteRef;
import com.uniquindio.teen_sport.dto.IdName;
import com.uniquindio.teen_sport.dto.RecurrentAbsence;
import com.uniquindio.teen_sport.entity.AttendanceStatus;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.repository.AthleteGroupMark;
import com.uniquindio.teen_sport.repository.AttendanceRecordRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.text.Collator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deportistas con ausencias consecutivas (R4), KPI de inasistencia recurrente del Proceso 2.
 *
 * <p>Por cada deportista y grupo se cuentan las marcas {@code ABSENT} seguidas desde la más reciente
 * hasta la primera {@code PRESENT}; esa marca da {@code lastAttendance}. Solo cuentan los deportistas
 * con matrícula vigente en el grupo, para que no aparezcan quienes ya salieron (decisión del
 * 2026-11-10), y las marcas desde {@code dateFrom} si se indica.</p>
 *
 * <p>La racha se calcula en memoria sobre una proyección ordenada: el volumen de la academia es
 * pequeño y así se evita SQL nativo con funciones de ventana (ADR-06).</p>
 */
@Service
public class RecurrentAbsenceService {

    /** Compara nombres en español sin distinguir tildes ni mayúsculas ("Álvarez" antes que "Pérez"). */
    private static final Collator NAMES = Collator.getInstance(Locale.forLanguageTag("es-CO"));

    static {
        NAMES.setStrength(Collator.PRIMARY);
    }

    private static final Comparator<RecurrentAbsence> ORDER =
            Comparator.comparingInt(RecurrentAbsence::consecutiveAbsences).reversed()
                    .thenComparing(absence -> absence.athlete().lastNames(), NAMES)
                    .thenComparing(absence -> absence.athlete().firstNames(), NAMES)
                    .thenComparing(absence -> absence.athlete().id())
                    .thenComparing(absence -> absence.group().id());

    private final AttendanceRecordRepository attendanceRepository;
    private final GroupAccessPolicy groupAccessPolicy;

    /**
     * @param attendanceRepository marcas de asistencia
     * @param groupAccessPolicy    alcance de los entrenadores
     */
    public RecurrentAbsenceService(AttendanceRecordRepository attendanceRepository,
                                   GroupAccessPolicy groupAccessPolicy) {
        this.attendanceRepository = attendanceRepository;
        this.groupAccessPolicy = groupAccessPolicy;
    }

    /**
     * R4: deportistas cuya racha actual de ausencias en un grupo alcanza el mínimo. Un grupo ajeno al
     * entrenador no devuelve datos, como en A9.
     *
     * @param user           identidad autenticada
     * @param minConsecutive ausencias seguidas mínimas, 1 o más
     * @param groupId        grupo, o {@code null}
     * @param dateFrom       primera fecha de sesión considerada, o {@code null}
     * @return los deportistas, de la racha más larga a la más corta y luego por apellidos y nombres
     * @throws BusinessException 403 {@code ACCESS_DENIED} si el rol no puede consultar grupos
     */
    @Transactional(readOnly = true)
    public List<RecurrentAbsence> recurrentAbsences(AuthenticatedUser user, int minConsecutive, Integer groupId,
                                                    LocalDate dateFrom) {
        Integer coachId = groupAccessPolicy.coachScope(user, null);
        List<AthleteGroupMark> marks = attendanceRepository.findMarksOfEnrolledAthletes(groupId, coachId, dateFrom,
                EnrollmentStatus.IN_FORCE);
        return marks.stream()
                .collect(Collectors.groupingBy(mark -> List.of(mark.getAthleteId(), mark.getGroupId()),
                        LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(RecurrentAbsenceService::currentStreak)
                .filter(absence -> absence.consecutiveAbsences() >= minConsecutive)
                .sorted(ORDER)
                .toList();
    }

    /**
     * @param marks marcas de un deportista en un grupo, de la más reciente a la más antigua
     */
    private static RecurrentAbsence currentStreak(List<AthleteGroupMark> marks) {
        AthleteGroupMark latest = marks.getFirst();
        int absences = 0;
        LocalDate lastAttendance = null;
        for (AthleteGroupMark mark : marks) {
            if (mark.getStatus() == AttendanceStatus.PRESENT) {
                lastAttendance = mark.getSessionDate();
                break;
            }
            absences++;
        }
        return new RecurrentAbsence(new AthleteRef(latest.getAthleteId(), latest.getFirstNames(),
                latest.getLastNames()), new IdName(latest.getGroupId(), latest.getGroupName()), absences,
                lastAttendance);
    }
}
