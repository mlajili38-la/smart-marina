package tn.iset.marina.notification_service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import tn.iset.marina.notification_service.entity.Notification;
import tn.iset.marina.notification_service.entity.NotificationType;
import tn.iset.marina.notification_service.repository.NotificationRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flux événementiel complet avec un vrai broker Kafka embarqué (sans Docker) et une base H2 :
 * le message JSON tel que publié par consumption-service devient une notification en base.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:notifications;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@EmbeddedKafka(partitions = 3, topics = {"consumption-recorded", "consumption-recorded-dlt"})
class KafkaFlowIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private NotificationRepository repository;

    @Test
    void consumptionEventBecomesNotifications() throws Exception {
        // JSON identique à celui produit par consumption-service (aucun en-tête de type)
        String json = """
                {"eventId":"evt-it-1","occurredAt":"%s","consumptionId":10,"pedestalId":5,
                 "pedestalSerialNumber":"XYZ-PED-005","portId":3,"type":"ELECTRICITY",
                 "quantity":75.5,"unit":"kWh","boatName":"Sea Breeze"}
                """.formatted(Instant.now());
        stringTemplate().send("consumption-recorded", "5", json).get();

        List<Notification> notifications = waitFor(() -> repository.findAll(), 2);

        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().anyMatch(n -> n.getType() == NotificationType.HIGH_CONSUMPTION));
        assertTrue(notifications.stream().allMatch(n -> n.getPortId() == 3L));
    }

    @Test
    void unreadableMessageGoesToDeadLetterTopic() throws Exception {
        stringTemplate().send("consumption-recorded", "9", "ceci n'est pas du JSON").get();

        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "dlt-reader", true);
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "consumption-recorded-dlt");
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, "consumption-recorded-dlt", Duration.ofSeconds(20));
            assertEquals("9", record.key());
        }
    }

    private KafkaTemplate<String, String> stringTemplate() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    private <T> List<T> waitFor(java.util.function.Supplier<List<T>> supplier, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        List<T> result = supplier.get();
        while (result.size() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
            result = supplier.get();
        }
        return result;
    }
}
