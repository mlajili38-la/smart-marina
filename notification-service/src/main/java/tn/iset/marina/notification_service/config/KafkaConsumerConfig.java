package tn.iset.marina.notification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    /**
     * En cas d'échec de traitement : 3 nouvelles tentatives espacées d'1 seconde,
     * puis le message est publié sur "<topic>-dlt" pour analyse, et la consommation continue
     * (suffixe par défaut depuis spring-kafka 4 ; c'était ".DLT" avant).
     * Un message illisible (désérialisation impossible) part directement en DLT, sans réessai.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }

    /** Le DLT doit avoir au moins autant de partitions que le topic source. */
    @Bean
    NewTopic consumptionRecordedDeadLetterTopic(
            @Value("${smart-marina.kafka.topics.consumption-recorded}") String topic) {
        return TopicBuilder.name(topic + "-dlt").partitions(3).replicas(1).build();
    }
}
