package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.DeliveryStatus;
import com.uniquindio.teen_sport.entity.Notification;
import com.uniquindio.teen_sport.repository.NotificationRepository;
import com.uniquindio.teen_sport.repository.UserRepository;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de las notificaciones en {@code notifications}. Cada escritura va en su propia transacción:
 * el envío ocurre fuera de la transacción del caso de uso, que ya terminó, y el estado de cada intento
 * debe quedar guardado aunque el siguiente falle.
 */
@Component
public class NotificationLog {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * @param notificationRepository notificaciones
     * @param userRepository         cuentas, para referenciar al destinatario
     * @param clock                  reloj de la aplicación
     */
    public NotificationLog(NotificationRepository notificationRepository, UserRepository userRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * @param request notificación por enviar
     * @return id de la notificación registrada como {@code PENDING}, sin intentos
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer open(NotificationRequested request) {
        Notification notification = new Notification(userRepository.getReferenceById(request.userId()),
                request.type(), request.message().subject(), clock.instant());
        return notificationRepository.save(notification).getId();
    }

    /**
     * @param notificationId id de la notificación
     * @param status         estado tras el intento
     * @param attempts       intentos realizados hasta ahora
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(Integer notificationId, DeliveryStatus status, int attempts) {
        notificationRepository.getReferenceById(notificationId).recordAttempt(status, attempts);
    }
}
