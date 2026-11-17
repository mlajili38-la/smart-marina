package tn.iset.marina.consumption_service.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Événement métier publié sur le topic Kafka "consumption-recorded"
 * quand une consommation est enregistrée (après validation de la transaction).
 * C'est un fait passé : il ne demande rien, il informe les autres services.
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
