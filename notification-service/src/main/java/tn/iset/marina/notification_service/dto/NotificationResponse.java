package tn.iset.marina.notification_service.dto;

import tn.iset.marina.notification_service.entity.NotificationLevel;
import tn.iset.marina.notification_service.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        NotificationLevel level,
        String message,
        Long portId,
        Long pedestalId,
        Long consumptionId,
        boolean read,
        Instant createdAt
) {
}
