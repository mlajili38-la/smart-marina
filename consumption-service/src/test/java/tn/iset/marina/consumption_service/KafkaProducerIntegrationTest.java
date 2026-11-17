package tn.iset.marina.consumption_service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tn.iset.marina.consumption_service.client.PedestalClient;
import tn.iset.marina.consumption_service.dto.ConsumptionRequest;
import tn.iset.marina.consumption_service.dto.PedestalResponse;
import tn.iset.marina.consumption_service.entity.ConsumptionType;
import tn.iset.marina.consumption_service.security.CurrentUser;
import tn.iset.marina.consumption_service.service.ConsumptionService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Enregistrement réel (H2) puis publication réelle sur un broker Kafka embarqué. */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:consumptions;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@EmbeddedKafka(partitions = 3, topics = "consumption-recorded")
class KafkaProducerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ConsumptionService service;

    @MockitoBean
    private PedestalClient pedestalClient;

    @Test
    void recordPublishesJsonEventAfterCommit() {
        when(pedestalClient.getPedestal(5L)).thenReturn(new PedestalResponse(5L, "XYZ-PED-005", "B5", 3L, "ACTIVE"));
        Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);

        var saved = service.record(new ConsumptionRequest(5L, ConsumptionType.WATER, new BigDecimal("0.750"),
                "Sea Breeze", end.minus(2, ChronoUnit.HOURS), end), CurrentUser.SYSTEM);

        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "test-reader", true);
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "consumption-recorded");
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, "consumption-recorded", Duration.ofSeconds(20));

            assertEquals("5", record.key());
            assertTrue(record.value().contains("\"consumptionId\":" + saved.id()));
            assertTrue(record.value().contains("\"type\":\"WATER\""));
            assertTrue(record.value().contains("\"pedestalSerialNumber\":\"XYZ-PED-005\""));
            // Pas d'en-tête __TypeId__ : le consommateur n'a pas besoin de nos classes Java
            assertFalse(record.headers().headers("__TypeId__").iterator().hasNext());
        }
    }
}
