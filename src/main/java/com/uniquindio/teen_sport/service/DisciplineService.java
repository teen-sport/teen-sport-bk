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
import com.uniquindio.teen_sport.validation.TextNormalizer;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del catálogo de disciplinas: listar (H1), consultar (H2), crear (H3), editar (H4)
 * y activar o desactivar (H5).
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>El nombre es único sin distinguir mayúsculas ("Fútbol" y "fútbol" son la misma disciplina).</li>
 *   <li>Toda disciplina se crea {@code ACTIVE}.</li>
 *   <li>Quien no es administrador solo ve disciplinas activas en el listado.</li>
 * </ul>
 */
@Service
public class DisciplineService {

    static final String NOT_FOUND_MESSAGE = "La disciplina no existe.";
    static final String DUPLICATE_MESSAGE = "Ya existe una disciplina con ese nombre.";
    static final String HAS_ACTIVE_GROUPS_MESSAGE =
            "La disciplina tiene grupos activos. Desactívalos antes de desactivar la disciplina.";

    /** Orden del listado: nombre y, para desempatar, id. */
    static final Sort ORDER = Sort.by("name", "id");

    private final DisciplineRepository disciplineRepository;
    private final GroupRepository groupRepository;

    /**
     * @param disciplineRepository catálogo de disciplinas
     * @param groupRepository      grupos de entrenamiento
     */
    public DisciplineService(DisciplineRepository disciplineRepository, GroupRepository groupRepository) {
        this.disciplineRepository = disciplineRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * H1: listado de disciplinas.
     *
     * @param requesterRole rol de quien consulta
     * @param status        filtro por estado; para quien no es administrador siempre es {@code ACTIVE}
     * @return disciplinas ordenadas por nombre
     */
    @Transactional(readOnly = true)
    public List<DisciplineResponse> list(RoleName requesterRole, EntityStatus status) {
        EntityStatus effectiveStatus = requesterRole == RoleName.ADMIN ? status : EntityStatus.ACTIVE;
        List<Discipline> disciplines = effectiveStatus == null
                ? disciplineRepository.findAll(ORDER)
                : disciplineRepository.findByStatus(effectiveStatus, ORDER);
        return disciplines.stream().map(DisciplineResponse::from).toList();
    }

    /**
     * H2: detalle de una disciplina.
     *
     * @param id id de la disciplina
     * @return la disciplina
     * @throws BusinessException 404 {@code NOT_FOUND}
     */
    @Transactional(readOnly = true)
    public DisciplineResponse get(Integer id) {
        return DisciplineResponse.from(find(id));
    }

    /**
     * H3: crea una disciplina activa.
     *
     * @param request datos de la disciplina
     * @return la disciplina creada
     * @throws BusinessException 409 {@code DUPLICATE_DISCIPLINE}
     */
    @Transactional
    public DisciplineResponse create(DisciplineRequest request) {
        String name = request.name().trim();
        if (disciplineRepository.existsByNameIgnoreCase(name)) {
            throw duplicate();
        }
        Discipline discipline = new Discipline(name, TextNormalizer.trimToNull(request.description()),
                request.monthlyFee());
        return DisciplineResponse.from(disciplineRepository.save(discipline));
    }

    /**
     * H4: edita nombre, descripción y mensualidad. La nueva mensualidad aplica a los pagos que se
     * registren desde este momento.
     *
     * @param id      id de la disciplina
     * @param request nuevos datos
     * @return la disciplina actualizada
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code DUPLICATE_DISCIPLINE}
     */
    @Transactional
    public DisciplineResponse update(Integer id, DisciplineRequest request) {
        Discipline discipline = find(id);
        String name = request.name().trim();
        if (disciplineRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicate();
        }
        discipline.setName(name);
        discipline.setDescription(TextNormalizer.trimToNull(request.description()));
        discipline.setMonthlyFee(request.monthlyFee());
        return DisciplineResponse.from(discipline);
    }

    /**
     * H5: activa o desactiva una disciplina.
     *
     * <p>No se puede desactivar una disciplina que tenga grupos activos.</p>
     *
     * @param id     id de la disciplina
     * @param status nuevo estado
     * @return la disciplina actualizada
     * @throws BusinessException 404 {@code NOT_FOUND}; 409 {@code DISCIPLINE_HAS_ACTIVE_GROUPS}
     */
    @Transactional
    public DisciplineResponse changeStatus(Integer id, EntityStatus status) {
        Discipline discipline = find(id);
        if (status == EntityStatus.INACTIVE && groupRepository.existsByDisciplineIdAndStatus(id, EntityStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.DISCIPLINE_HAS_ACTIVE_GROUPS, HttpStatus.CONFLICT,
                    HAS_ACTIVE_GROUPS_MESSAGE);
        }
        discipline.setStatus(status);
        return DisciplineResponse.from(discipline);
    }

    private Discipline find(Integer id) {
        return disciplineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
    }

    private static BusinessException duplicate() {
        return new BusinessException(ErrorCode.DUPLICATE_DISCIPLINE, HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
    }
}
