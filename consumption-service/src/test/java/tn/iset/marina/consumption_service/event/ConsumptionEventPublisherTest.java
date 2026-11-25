package tn.iset.marina.consumption_service.event;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumptionEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void sendsEventOnTopicWithPedestalIdAsKey() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        ConsumptionRecordedEvent event = new ConsumptionRecordedEvent("evt-1", Instant.now(), 10L, 5L,
                "XYZ-PED-005", 3L, "WATER", BigDecimal.ONE, "m3", null);
        when(template.send("consumption-recorded", "5", event)).thenReturn(new CompletableFuture<>());

        new ConsumptionEventPublisher(template, "consumption-recorded").publish(event);

        verify(template).send("consumption-recorded", "5", event);
    }
}
