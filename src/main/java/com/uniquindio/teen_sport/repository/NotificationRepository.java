package com.uniquindio.teen_sport.repository;

import com.uniquindio.teen_sport.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso al registro de notificaciones.
 */
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
}
