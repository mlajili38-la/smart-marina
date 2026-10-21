package tn.iset.marina.pedestal_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient portRestClient(@Value("${port-service.base-url}") String baseUrl) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        // On crée le builder nous-mêmes : en Spring Boot 4, il n'est plus fourni comme bean par défaut
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}