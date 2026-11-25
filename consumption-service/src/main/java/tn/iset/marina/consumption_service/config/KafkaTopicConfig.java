package tn.iset.marina.consumption_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /** Le topic est créé au démarrage s'il n'existe pas (3 partitions, 1 réplique en développement). */
    @Bean
    NewTopic consumptionRecordedTopic(@Value("${smart-marina.kafka.topics.consumption-recorded}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
