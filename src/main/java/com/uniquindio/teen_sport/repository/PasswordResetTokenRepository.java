package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Tokens de recuperación de contraseña.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    /**
     * Bloquea el token hasta el fin de la transacción: dos restablecimientos simultáneos con el mismo
     * token se serializan y solo el primero lo encuentra sin usar.
     *
     * @param tokenHash hash SHA-256 del token, en hexadecimal
     * @return el token con su cuenta cargada
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findLockedByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Invalida los tokens sin usar y aún vigentes de una cuenta adelantando su vencimiento a {@code now}.
     * Antes vuelca los cambios pendientes, para no pisar un token marcado como usado en la misma
     * transacción.
     *
     * @param userId cuenta
     * @param now    momento de la invalidación
     * @return tokens invalidados
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update PasswordResetToken t set t.expiresAt = :now
            where t.user.id = :userId and t.usedAt is null and t.expiresAt > :now
            """)
    int expireOpenTokens(@Param("userId") Integer userId, @Param("now") Instant now);
}
