package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.DeliveryStatus;
import com.uniquindio.teen_sport.entity.NotificationType;
import com.uniquindio.teen_sport.integration.EmailDeliveryException;
import com.uniquindio.teen_sport.integration.EmailMessage;
import com.uniquindio.teen_sport.integration.EmailSender;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ciclo de vida de una notificación: registro, intentos y estado final.
 */
class NotificationDispatcherTest {

    private static final EmailMessage MESSAGE = new EmailMessage("g@teensport.test", "Laura Ríos", "Asunto", "<p/>");
    private static final NotificationRequested REQUEST =
            new NotificationRequested(30, NotificationType.ACCOUNT_REGISTERED, MESSAGE, false);

    private final EmailSender emailSender = mock(EmailSender.class);
    private final NotificationLog notificationLog = mock(NotificationLog.class);

    @Test
    void aSuccessfulFirstAttemptIsSent() {
        when(notificationLog.open(REQUEST)).thenReturn(1);

        dispatcher(3, Duration.ZERO).deliver(REQUEST);

        InOrder order = inOrder(notificationLog, emailSender);
        order.verify(notificationLog).open(REQUEST);
        order.verify(emailSender).send(MESSAGE);
        order.verify(notificationLog).recordAttempt(1, DeliveryStatus.SENT, 1);
    }

    @Test
    void transientFailuresAreRetriedUntilTheProviderAccepts() {
        when(notificationLog.open(REQUEST)).thenReturn(1);
        doThrow(new EmailDeliveryException("503", true)).doNothing().when(emailSender).send(MESSAGE);

        dispatcher(3, Duration.ZERO).deliver(REQUEST);

        InOrder order = inOrder(notificationLog);
        order.verify(notificationLog).recordAttempt(1, DeliveryStatus.PENDING, 1);
        order.verify(notificationLog).recordAttempt(1, DeliveryStatus.SENT, 2);
        verify(emailSender, times(2)).send(MESSAGE);
    }

    @Test
    void afterTheLastAttemptTheNotificationFails() {
        when(notificationLog.open(REQUEST)).thenReturn(1);
        doThrow(new EmailDeliveryException("503", true)).when(emailSender).send(MESSAGE);

        dispatcher(3, Duration.ZERO).deliver(REQUEST);

        verify(emailSender, times(3)).send(MESSAGE);
        verify(notificationLog).recordAttempt(1, DeliveryStatus.PENDING, 1);
        verify(notificationLog).recordAttempt(1, DeliveryStatus.PENDING, 2);
        verify(notificationLog).recordAttempt(1, DeliveryStatus.FAILED, 3);
        verify(notificationLog, never()).recordAttempt(1, DeliveryStatus.SENT, 3);
    }

    @Test
    void aPermanentFailureIsNotRetried() {
        when(notificationLog.open(REQUEST)).thenReturn(1);
        doThrow(new EmailDeliveryException("401", false)).when(emailSender).send(MESSAGE);

        dispatcher(3, Duration.ZERO).deliver(REQUEST);

        verify(emailSender).send(MESSAGE);
        verify(notificationLog).recordAttempt(1, DeliveryStatus.FAILED, 1);
    }

    @Test
    void anInterruptedWaitStopsRetryingAndFails() {
        when(notificationLog.open(REQUEST)).thenReturn(1);
        doThrow(new EmailDeliveryException("503", true)).when(emailSender).send(MESSAGE);
        Thread.currentThread().interrupt();

        dispatcher(3, Duration.ofSeconds(30)).deliver(REQUEST);

        assertThat(Thread.interrupted()).isTrue();
        verify(emailSender).send(MESSAGE);
        verify(notificationLog).recordAttempt(1, DeliveryStatus.PENDING, 1);
        verify(notificationLog).recordAttempt(1, DeliveryStatus.FAILED, 1);
    }

    @Test
    void theWaitGrowsWithEachAttempt() {
        when(notificationLog.open(REQUEST)).thenReturn(1);
        doThrow(new EmailDeliveryException("503", true)).doNothing().when(emailSender).send(MESSAGE);
        long start = System.nanoTime();

        dispatcher(2, Duration.ofMillis(50)).deliver(REQUEST);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(50));
        verify(notificationLog).recordAttempt(1, DeliveryStatus.SENT, 2);
    }

    @Test
    void unexpectedErrorsAreNotHiddenAsSent() {
        when(notificationLog.open(REQUEST)).thenReturn(1);
        doThrow(new IllegalStateException("fallo inesperado")).when(emailSender).send(any());

        NotificationDispatcher dispatcher = dispatcher(3, Duration.ZERO);

        assertThatThrownBy(() -> dispatcher.deliver(REQUEST)).isInstanceOf(IllegalStateException.class);
        verify(notificationLog, never()).recordAttempt(1, DeliveryStatus.SENT, 1);
    }

    @Test
    void listenersDeliverTheRequest() {
        when(notificationLog.open(REQUEST)).thenReturn(1);
        doNothing().when(emailSender).send(MESSAGE);
        NotificationDispatcher dispatcher = dispatcher(1, Duration.ZERO);

        dispatcher.onCommit(REQUEST);
        dispatcher.onRollback(REQUEST);

        verify(emailSender, times(2)).send(MESSAGE);
    }

    @Test
    void invalidRetryPoliciesAreRejectedAtStartup() {
        Duration negative = Duration.ofSeconds(-1);
        assertThatThrownBy(() -> new NotificationProperties(0, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationProperties(3, negative))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationProperties(3, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private NotificationDispatcher dispatcher(int maxAttempts, Duration retryDelay) {
        return new NotificationDispatcher(emailSender, notificationLog,
                new NotificationProperties(maxAttempts, retryDelay));
    }
}
