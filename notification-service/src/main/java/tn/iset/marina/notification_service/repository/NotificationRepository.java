package tn.iset.marina.notification_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.iset.marina.notification_service.entity.Notification;
import tn.iset.marina.notification_service.entity.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsBySourceEventIdAndType(String sourceEventId, NotificationType type);

    @Query("""
            select n from Notification n
            where (:portId is null or n.portId = :portId)
              and (:unreadOnly = false or n.read = false)
            """)
    Page<Notification> search(Long portId, boolean unreadOnly, Pageable pageable);
}
