package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.config.AcademyCalendar;
import com.uniquindio.teen_sport.entity.Athlete;
import com.uniquindio.teen_sport.entity.Coach;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.Enrollment;
import com.uniquindio.teen_sport.entity.EnrollmentStatus;
import com.uniquindio.teen_sport.entity.Group;
import com.uniquindio.teen_sport.entity.Guardian;
import com.uniquindio.teen_sport.entity.Role;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.TrainingSession;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Datos de prueba de sesiones y asistencia, sin base de datos.
 */
final class SessionFixtures {

    /** 13 de octubre en UTC, pero todavía 12 de octubre en Bogotá. */
    static final Instant NOW = Instant.parse("2026-10-13T03:00:00Z");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, RoleName.ADMIN, null, null);
    static final AuthenticatedUser COACH = new AuthenticatedUser(2, RoleName.COACH, null, 10);
    static final AuthenticatedUser OTHER_COACH = new AuthenticatedUser(3, RoleName.COACH, null, 11);
    static final AuthenticatedUser GUARDIAN = new AuthenticatedUser(4, RoleName.GUARDIAN, 20, null);

    private SessionFixtures() {
    }

    static AcademyCalendar calendar() {
        return new AcademyCalendar(Clock.fixed(NOW, ZoneOffset.UTC), "America/Bogota");
    }

    /** Grupo 7 de la disciplina 3, a cargo del entrenador 10. */
    static Group group() {
        User user = new User(new Role(RoleName.COACH, null), "c@teensport.test", "$2a$hash", NOW);
        user.setFirstName("Luis");
        user.setLastName("Rojas");
        Coach coach = new Coach(user, null, null);
        ReflectionTestUtils.setField(coach, "id", 10);
        Discipline discipline = new Discipline("Fútbol", null, BigDecimal.TEN);
        ReflectionTestUtils.setField(discipline, "id", 3);
        Group group = new Group(discipline, coach, "Grupo 7", 15);
        ReflectionTestUtils.setField(group, "id", 7);
        return group;
    }

    static TrainingSession session(int id, Group group, LocalDate date) {
        TrainingSession session = new TrainingSession(group, date, LocalTime.of(16, 0), LocalTime.of(17, 0));
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    static Athlete athlete(int id, String firstName, String lastName) {
        Guardian guardian = new Guardian(new User(new Role(RoleName.GUARDIAN, null), "g@teensport.test", "$2a$hash",
                NOW), null);
        ReflectionTestUtils.setField(guardian, "id", 20);
        Athlete athlete = new Athlete(guardian, NOW);
        athlete.setFirstName(firstName);
        athlete.setLastName(lastName);
        ReflectionTestUtils.setField(athlete, "id", id);
        return athlete;
    }

    static Enrollment activeEnrollment(Athlete athlete, Group group, YearMonth period) {
        Enrollment enrollment = new Enrollment(athlete, group, period, NOW);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }
}
