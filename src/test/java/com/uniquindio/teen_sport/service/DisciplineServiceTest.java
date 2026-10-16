package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.DisciplineRequest;
import com.uniquindio.teen_sport.dto.DisciplineResponse;
import com.uniquindio.teen_sport.entity.Discipline;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.DisciplineRepository;
import com.uniquindio.teen_sport.repository.GroupRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisciplineServiceTest {

    private static final BigDecimal FEE = new BigDecimal("120000.00");

    private final DisciplineRepository repository = mock(DisciplineRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final DisciplineService service = new DisciplineService(repository, groupRepository);

    @BeforeEach
    void setUp() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsActiveDisciplineWithTrimmedData() {
        DisciplineResponse created = service.create(new DisciplineRequest("  Fútbol ", "  ", FEE));

        ArgumentCaptor<Discipline> saved = ArgumentCaptor.forClass(Discipline.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Fútbol");
        assertThat(saved.getValue().getDescription()).isNull();
        assertThat(created.status()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(created.monthlyFee()).isEqualByComparingTo("120000");
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        when(repository.existsByNameIgnoreCase("fútbol")).thenReturn(true);

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.create(new DisciplineRequest("fútbol", null, FEE)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.DUPLICATE_DISCIPLINE);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesDataAndAllowsKeepingItsOwnName() {
        Discipline discipline = existing(3, "Fútbol");

        DisciplineResponse updated = service.update(3, new DisciplineRequest("Fútbol", "Sub-15", new BigDecimal("150000")));

        assertThat(updated.description()).isEqualTo("Sub-15");
        assertThat(discipline.getMonthlyFee()).isEqualByComparingTo("150000");
        verify(repository).existsByNameIgnoreCaseAndIdNot("Fútbol", 3);
    }

    @Test
    void updateRejectsNameOfAnotherDiscipline() {
        existing(3, "Fútbol");
        when(repository.existsByNameIgnoreCaseAndIdNot("Natación", 3)).thenReturn(true);

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.update(3, new DisciplineRequest("Natación", null, FEE)));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.DUPLICATE_DISCIPLINE);
    }

    @Test
    void missingDisciplineIsNotFound() {
        when(repository.findById(9)).thenReturn(Optional.empty());

        BusinessException ex = catchThrowableOfType(BusinessException.class, () -> service.get(9));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void changesStatus() {
        Discipline discipline = existing(3, "Fútbol");

        assertThat(service.changeStatus(3, EntityStatus.INACTIVE).status()).isEqualTo(EntityStatus.INACTIVE);
        assertThat(discipline.getStatus()).isEqualTo(EntityStatus.INACTIVE);
    }

    @Test
    void disciplineWithActiveGroupsCannotBeDeactivatedButCanBeActivated() {
        Discipline discipline = existing(3, "Fútbol");
        when(groupRepository.existsByDisciplineIdAndStatus(3, EntityStatus.ACTIVE)).thenReturn(true);

        BusinessException ex = catchThrowableOfType(BusinessException.class,
                () -> service.changeStatus(3, EntityStatus.INACTIVE));

        assertThat(ex.getCode()).isEqualTo(ErrorCode.DISCIPLINE_HAS_ACTIVE_GROUPS);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(discipline.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(service.changeStatus(3, EntityStatus.ACTIVE).status()).isEqualTo(EntityStatus.ACTIVE);
    }

    @Test
    void adminListsAllOrFiltersByStatus() {
        List<Discipline> all = List.of(existing(1, "A"), existing(2, "B"));
        when(repository.findAll(DisciplineService.ORDER)).thenReturn(all);
        when(repository.findByStatus(EntityStatus.INACTIVE, DisciplineService.ORDER)).thenReturn(List.of());

        assertThat(service.list(RoleName.ADMIN, null)).hasSize(2);
        assertThat(service.list(RoleName.ADMIN, EntityStatus.INACTIVE)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"COACH", "GUARDIAN"})
    void nonAdminsOnlySeeActiveDisciplinesWhateverTheyAskFor(RoleName role) {
        List<Discipline> active = List.of(existing(1, "A"));
        when(repository.findByStatus(EntityStatus.ACTIVE, DisciplineService.ORDER)).thenReturn(active);

        assertThat(service.list(role, EntityStatus.INACTIVE)).hasSize(1);
        assertThat(service.list(role, null)).hasSize(1);
        verify(repository, never()).findByStatus(EntityStatus.INACTIVE, DisciplineService.ORDER);
        verify(repository, never()).findAll(DisciplineService.ORDER);
    }

    private Discipline existing(int id, String name) {
        Discipline discipline = new Discipline(name, null, FEE);
        ReflectionTestUtils.setField(discipline, "id", id);
        when(repository.findById(id)).thenReturn(Optional.of(discipline));
        return discipline;
    }
}
