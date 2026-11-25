package tn.iset.marina.notification_service.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import tn.iset.marina.notification_service.entity.Notification;
import tn.iset.marina.notification_service.entity.NotificationLevel;
import tn.iset.marina.notification_service.entity.NotificationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    @Test
    void searchFiltersByPortAndReadState() {
        repository.save(notification("e1", 3L, false));
        repository.save(notification("e2", 3L, true));
        repository.save(notification("e3", 7L, false));

        assertEquals(3, repository.search(null, false, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, repository.search(3L, false, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, repository.search(3L, true, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void sameEventAndTypeCannotBeStoredTwice() {
        repository.saveAndFlush(notification("dup", 3L, false));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(notification("dup", 3L, false)));
    }

    private Notification notification(String eventId, Long portId, boolean read) {
        Notification notification = new Notification();
        notification.setType(NotificationType.CONSUMPTION_RECORDED);
        notification.setLevel(NotificationLevel.INFO);
        notification.setMessage("test");
        notification.setPortId(portId);
        notification.setSourceEventId(eventId);
        notification.setRead(read);
        return notification;
    }
}
