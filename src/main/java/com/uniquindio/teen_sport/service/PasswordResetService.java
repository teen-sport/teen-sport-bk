package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.PasswordResetConfirmRequest;
import com.uniquindio.teen_sport.entity.PasswordResetToken;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.repository.PasswordResetTokenRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import com.uniquindio.teen_sport.validation.EmailNormalizer;
import com.uniquindio.teen_sport.validation.PasswordPolicy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Casos de uso U13 y U14: recuperación de contraseña por correo (decisiones del 2026-11-10).
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>La solicitud responde igual exista o no el correo y esté o no activa la cuenta. Solo una cuenta
 *       activa recibe el token, y el correo se envía después del commit, fuera de la petición.</li>
 *   <li>El token es aleatorio (256 bits), vale 20 minutos y una sola vez. Se guarda solo su hash SHA-256;
 *       el token en claro solo viaja en el enlace del correo.</li>
 *   <li>Una solicitud nueva invalida los tokens anteriores de la cuenta; un restablecimiento, todos los
 *       demás.</li>
 *   <li>Un token inexistente, vencido, usado, reemplazado o de una cuenta inactiva responde el mismo 422
 *       {@code INVALID_RESET_TOKEN}. La política de contraseñas se valida después del token, así que un
 *       token inválido no revela nada más; con una contraseña débil el token no se consume.</li>
 *   <li>Los JWT ya emitidos siguen válidos hasta su vencimiento (no hay revocación).</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    static final String INVALID_TOKEN_MESSAGE = "El enlace de recuperación no es válido o ya venció. "
            + "Solicita uno nuevo.";

    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final NotificationService notificationService;
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /**
     * @param userRepository      cuentas
     * @param tokenRepository     tokens de recuperación
     * @param passwordEncoder     hash BCrypt de la nueva contraseña
     * @param passwordPolicy      fortaleza de contraseñas
     * @param notificationService correo con el enlace
     * @param properties          página del enlace y vigencia del token
     * @param clock               reloj de la aplicación
     */
    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
                                NotificationService notificationService, PasswordResetProperties properties,
                                Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.notificationService = notificationService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * U13: emite un token y envía el enlace si el correo es de una cuenta activa; si no, no hace nada.
     *
     * @param email correo tal como llegó
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(EmailNormalizer.normalize(email))
                .filter(User::isActive)
                .ifPresent(this::issueToken);
    }

    /**
     * U14: cambia la contraseña con un token válido y lo consume.
     *
     * @param request token y nueva contraseña
     * @throws BusinessException 422 {@code INVALID_RESET_TOKEN} o {@code WEAK_PASSWORD}
     */
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository.findLockedByTokenHash(hash(request.token()))
                .filter(candidate -> candidate.isUsableAt(now) && candidate.getUser().isActive())
                .orElseThrow(PasswordResetService::invalidToken);
        passwordPolicy.validate(request.newPassword());
        token.markUsed(now);
        User user = token.getUser();
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        tokenRepository.expireOpenTokens(user.getId(), now);
    }

    private void issueToken(User user) {
        Instant now = clock.instant();
        tokenRepository.expireOpenTokens(user.getId(), now);
        String token = newToken();
        tokenRepository.save(new PasswordResetToken(user, hash(token), now.plus(properties.tokenValidity()), now));
        notificationService.passwordResetRequested(user, resetLink(token), properties.tokenValidity());
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resetLink(String token) {
        return UriComponentsBuilder.fromUriString(properties.url()).queryParam("token", token).encode().build()
                .toUriString();
    }

    /**
     * @param token token en claro
     * @return hash SHA-256 en hexadecimal, tal como se guarda en {@code token_hash}
     */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La plataforma Java no ofrece SHA-256", e);
        }
    }

    private static BusinessException invalidToken() {
        return new BusinessException(ErrorCode.INVALID_RESET_TOKEN, HttpStatus.UNPROCESSABLE_CONTENT,
                INVALID_TOKEN_MESSAGE);
    }
}
