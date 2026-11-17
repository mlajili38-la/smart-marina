package tn.iset.marina.notification_service.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.marina.notification_service.config.NotificationThresholds;
import tn.iset.marina.notification_service.dto.NotificationPageResponse;
import tn.iset.marina.notification_service.dto.NotificationResponse;
import tn.iset.marina.notification_service.entity.Notification;
import tn.iset.marina.notification_service.entity.NotificationLevel;
import tn.iset.marina.notification_service.entity.NotificationType;
import tn.iset.marina.notification_service.event.ConsumptionRecordedEvent;
import tn.iset.marina.notification_service.exception.ForbiddenPortAccessException;
import tn.iset.marina.notification_service.exception.NotificationNotFoundException;
import tn.iset.marina.notification_service.repository.NotificationRepository;
import tn.iset.marina.notification_service.security.CurrentUser;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationThresholds thresholds;

    /** Traite un événement Kafka. Idempotent : un événement redélivré ne crée pas de doublon. */
    @Transactional
    public void handle(ConsumptionRecordedEvent event) {
        String label = "ELECTRICITY".equals(event.type()) ? "électricité" : "eau";
        String boat = event.boatName() == null ? "" : " (bateau " + event.boatName() + ")";

        create(event, NotificationType.CONSUMPTION_RECORDED, NotificationLevel.INFO,
                "Consommation " + label + " de " + event.quantity().toPlainString() + " " + event.unit()
                        + " enregistrée sur la borne " + event.pedestalSerialNumber() + boat);

        BigDecimal threshold = thresholds.forType(event.type());
        if (threshold != null && event.quantity().compareTo(threshold) > 0) {
            create(event, NotificationType.HIGH_CONSUMPTION, NotificationLevel.WARNING,
                    "Consommation " + label + " élevée sur la borne " + event.pedestalSerialNumber() + " : "
                            + event.quantity().toPlainString() + " " + event.unit()
                            + " (seuil " + threshold.toPlainString() + " " + event.unit() + ")" + boat);
        }
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse findAll(Pageable pageable, Long portId, boolean unreadOnly, CurrentUser user) {
        Long effectivePortId = restrictPort(portId, user);
        Page<Notification> page = repository.search(effectivePortId, unreadOnly, pageable);
        return new NotificationPageResponse(page.map(this::toResponse).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, CurrentUser user) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        if (user.isRestrictedToOwnPort() && !user.canManagePort(notification.getPortId())) {
            throw new ForbiddenPortAccessException(notification.getPortId());
        }
        notification.setRead(true);
        return toResponse(notification);
    }

    /** RG-09 : un gestionnaire ne voit que les notifications de son port. */
    private Long restrictPort(Long requestedPortId, CurrentUser user) {
        if (!user.isRestrictedToOwnPort()) {
            return requestedPortId;
        }
        if (requestedPortId != null && !requestedPortId.equals(user.portId())) {
            throw new ForbiddenPortAccessException(requestedPortId);
        }
        return user.portId();
    }

    private void create(ConsumptionRecordedEvent event, NotificationType type, NotificationLevel level,
                        String message) {
        if (repository.existsBySourceEventIdAndType(event.eventId(), type)) {
            logger.info("Événement {} déjà traité ({}) : ignoré", event.eventId(), type);
            return;
        }
        Notification notification = new Notification();
        notification.setType(type);
        notification.setLevel(level);
        notification.setMessage(message);
        notification.setPortId(event.portId());
        notification.setPedestalId(event.pedestalId());
        notification.setConsumptionId(event.consumptionId());
        notification.setSourceEventId(event.eventId());
        repository.save(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getLevel(),
                notification.getMessage(), notification.getPortId(), notification.getPedestalId(),
                notification.getConsumptionId(), notification.isRead(), notification.getCreatedAt());
    }
}
