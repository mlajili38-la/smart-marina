package tn.iset.marina.notification_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iset.marina.notification_service.config.NotificationThresholds;
import tn.iset.marina.notification_service.entity.Notification;
import tn.iset.marina.notification_service.entity.NotificationLevel;
import tn.iset.marina.notification_service.entity.NotificationType;
import tn.iset.marina.notification_service.event.ConsumptionRecordedEvent;
import tn.iset.marina.notification_service.exception.ForbiddenPortAccessException;
import tn.iset.marina.notification_service.repository.NotificationRepository;
import tn.iset.marina.notification_service.security.CurrentUser;

import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository,
                new NotificationThresholds(new BigDecimal("50"), new BigDecimal("2")));
    }

    @Test
    void normalConsumptionCreatesOneInfoNotification() {
        service.handle(event("ELECTRICITY", "12.5", "kWh"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertEquals(NotificationType.CONSUMPTION_RECORDED, saved.getValue().getType());
        assertEquals(NotificationLevel.INFO, saved.getValue().getLevel());
        assertEquals(3L, saved.getValue().getPortId());
        assertTrue(saved.getValue().getMessage().contains("12.5 kWh"));
    }

    @Test
    void consumptionAboveThresholdAlsoCreatesWarning() {
        service.handle(event("WATER", "3.5", "m3"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(saved.capture());
        List<Notification> notifications = saved.getAllValues();
        assertEquals(NotificationType.HIGH_CONSUMPTION, notifications.get(1).getType());
        assertEquals(NotificationLevel.WARNING, notifications.get(1).getLevel());
        assertTrue(notifications.get(1).getMessage().contains("seuil 2 m3"));
    }

    @Test
    void redeliveredEventIsIgnored() {
        when(repository.existsBySourceEventIdAndType("evt-1", NotificationType.CONSUMPTION_RECORDED))
                .thenReturn(true);

        service.handle(event("ELECTRICITY", "1", "kWh"));

        verify(repository, never()).save(any());
    }

    @Test
    void managerCannotReadAnotherPortNotifications() {
        CurrentUser manager = CurrentUser.of("u1", "PORT_MANAGER", "3");

        assertThrows(ForbiddenPortAccessException.class,
                () -> service.findAll(PageRequest.of(0, 20), 7L, false, manager));
    }

    @Test
    void markAsReadUpdatesFlag() {
        Notification notification = new Notification();
        notification.setId(4L);
        notification.setPortId(3L);
        notification.setType(NotificationType.CONSUMPTION_RECORDED);
        notification.setLevel(NotificationLevel.INFO);
        when(repository.findById(4L)).thenReturn(Optional.of(notification));

        var response = service.markAsRead(4L, CurrentUser.of("u1", "PORT_MANAGER", "3"));

        assertTrue(response.read());
    }

    private ConsumptionRecordedEvent event(String type, String quantity, String unit) {
        return new ConsumptionRecordedEvent("evt-1", Instant.now(), 10L, 5L, "XYZ-PED-005", 3L,
                type, new BigDecimal(quantity), unit, "Sea Breeze");
    }
}
