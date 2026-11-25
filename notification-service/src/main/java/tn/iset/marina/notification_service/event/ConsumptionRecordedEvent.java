package tn.iset.marina.notification_service.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Copie locale du contrat de l'événement publié par consumption-service.
 * Chaque service garde sa propre classe : aucune dépendance de code entre microservices,
 * seul le format JSON est partagé.
 */
public record ConsumptionRecordedEvent(
        String eventId,
        Instant occurredAt,
        Long consumptionId,
        Long pedestalId,
        String pedestalSerialNumber,
        Long portId,
        String type,
        BigDecimal quantity,
        String unit,
        String boatName
) {
}
