package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.AthleteAttendanceReport;
import com.uniquindio.teen_sport.dto.AttendanceReport;
import com.uniquindio.teen_sport.dto.EnrollmentReport;
import com.uniquindio.teen_sport.dto.RecurrentAbsence;
import com.uniquindio.teen_sport.security.CurrentUserService;
import com.uniquindio.teen_sport.service.AttendanceReportService;
import com.uniquindio.teen_sport.service.EnrollmentReportService;
import com.uniquindio.teen_sport.service.RecurrentAbsenceService;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes e indicadores de matrícula y asistencia (R1–R4). El alcance por rol se aplica en los
 * servicios.
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final EnrollmentReportService enrollmentReportService;
    private final AttendanceReportService attendanceReportService;
    private final RecurrentAbsenceService recurrentAbsenceService;
    private final CurrentUserService currentUserService;

    /**
     * @param enrollmentReportService informe de matrículas
     * @param attendanceReportService informes de asistencia
     * @param recurrentAbsenceService ausencias consecutivas
     * @param currentUserService      identidad del usuario autenticado
     */
    public ReportController(EnrollmentReportService enrollmentReportService,
                            AttendanceReportService attendanceReportService,
                            RecurrentAbsenceService recurrentAbsenceService, CurrentUserService currentUserService) {
        this.enrollmentReportService = enrollmentReportService;
        this.attendanceReportService = attendanceReportService;
        this.recurrentAbsenceService = recurrentAbsenceService;
        this.currentUserService = currentUserService;
    }

    /**
     * R1: indicadores de matrícula de un periodo.
     *
     * @param period mes de inicio de las matrículas, {@code YYYY-MM}
     * @return 200 con el informe
     */
    @GetMapping("/enrollments")
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentReport getEnrollmentReport(@RequestParam YearMonth period) {
        return enrollmentReportService.enrollmentReport(period);
    }

    /**
     * R2: indicadores de asistencia.
     *
     * @param dateFrom     primera fecha, incluida; por defecto, el primer día del mes de {@code dateTo}
     * @param dateTo       última fecha, incluida; por defecto, hoy
     * @param groupId      filtro por grupo
     * @param disciplineId filtro por disciplina
     * @return 200 con el informe
     */
    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public AttendanceReport getAttendanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer disciplineId) {
        return attendanceReportService.attendanceReport(currentUserService.current(), dateFrom, dateTo, groupId,
                disciplineId);
    }

    /**
     * R3: asistencia de un deportista.
     *
     * @param id       id del deportista
     * @param dateFrom primera fecha, incluida; sin ella, desde el inicio del historial
     * @param dateTo   última fecha, incluida; sin ella, hasta el final del historial
     * @return 200 con el informe
     */
    @GetMapping("/attendance/athletes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH', 'GUARDIAN')")
    public AthleteAttendanceReport getAthleteAttendanceReport(
            @PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return attendanceReportService.athleteReport(currentUserService.current(), id, dateFrom, dateTo);
    }

    /**
     * R4: deportistas con ausencias consecutivas.
     *
     * @param minConsecutive ausencias seguidas mínimas, 1 o más
     * @param groupId        filtro por grupo
     * @param dateFrom       primera fecha de sesión considerada
     * @return 200 con los deportistas
     */
    @GetMapping("/attendance/recurrent-absences")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    public List<RecurrentAbsence> getRecurrentAbsences(
            @RequestParam(defaultValue = "3") @Min(1) int minConsecutive,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom) {
        return recurrentAbsenceService.recurrentAbsences(currentUserService.current(), minConsecutive, groupId,
                dateFrom);
    }
}
