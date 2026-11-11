package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las cuentas de usuario.
 */
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    /**
     * Página de cuentas filtradas, con el rol cargado en la misma consulta.
     *
     * @param specification filtros
     * @param pageable      página y orden
     * @return la página de cuentas
     */
    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Specification<User> specification, Pageable pageable);

    /**
     * Busca una cuenta por correo cargando también su rol, que siempre se necesita al autenticar.
     *
     * @param email correo normalizado
     * @return la cuenta, si existe
     */
    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);

    /**
     * @param email correo normalizado
     * @return {@code true} si otra cuenta ya usa el correo
     */
    boolean existsByEmail(String email);

    /**
     * @param documentNumber número de documento
     * @return {@code true} si otra cuenta ya usa el documento
     */
    boolean existsByDocumentNumber(String documentNumber);

    /**
     * @param roleName rol buscado
     * @return {@code true} si existe al menos una cuenta con ese rol
     */
    boolean existsByRoleName(RoleName roleName);

    /**
     * Cuentas creadas en un intervalo, por rol (R5). Los roles sin cuentas no aparecen.
     *
     * @param from primer instante, incluido
     * @param to   instante final, excluido
     * @return el número de cuentas de cada rol
     */
    @Query("""
            select u.role.name as role, count(u) as total from User u
            where u.createdAt >= :from and u.createdAt < :to
            group by u.role.name
            """)
    List<RoleCount> countCreatedByRole(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Cuentas por rol y estado actual (R5). Las combinaciones sin cuentas no aparecen.
     *
     * @return el número de cuentas de cada rol y estado
     */
    @Query("select u.role.name as role, u.status as status, count(u) as total from User u group by u.role.name, u.status")
    List<RoleStatusCount> countByRoleAndStatus();
}
