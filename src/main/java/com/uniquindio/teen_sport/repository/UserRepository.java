package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
