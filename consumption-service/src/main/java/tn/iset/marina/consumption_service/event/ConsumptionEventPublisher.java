package tn.iset.marina.consumption_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Producteur Kafka. L'événement n'est envoyé qu'APRÈS le commit en base :
 * si l'enregistrement échoue, aucune notification fantôme n'est publiée.
 * La clé du message est l'identifiant de la borne : les événements d'une même borne restent ordonnés.
 */
@Component
public class ConsumptionEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ConsumptionEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ConsumptionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                     @Value("${smart-marina.kafka.topics.consumption-recorded}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ConsumptionRecordedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.pedestalId()), event)
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        logger.error("Échec de publication de l'événement {} (consommation {})",
                                event.eventId(), event.consumptionId(), failure);
                    } else {
                        logger.info("Événement {} publié sur {} (partition {})", event.eventId(), topic,
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
