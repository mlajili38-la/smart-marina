package tn.iset.marina.notification_service.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tn.iset.marina.notification_service.service.NotificationService;

/** Consommateur Kafka : réagit aux consommations enregistrées par consumption-service. */
@Component
@RequiredArgsConstructor
public class ConsumptionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsumptionEventListener.class);

    private final NotificationService notificationService;

    @KafkaListener(topics = "${smart-marina.kafka.topics.consumption-recorded}")
    public void onConsumptionRecorded(ConsumptionRecordedEvent event) {
        logger.info("Événement reçu {} : consommation {} sur la borne {}",
                event.eventId(), event.consumptionId(), event.pedestalId());
        notificationService.handle(event);
    }
}
