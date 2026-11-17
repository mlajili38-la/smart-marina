package tn.iset.marina.pedestal_service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.iset.marina.pedestal_service.dto.ConsumptionSummaryResponse;
import tn.iset.marina.pedestal_service.exception.ConsumptionServiceUnavailableException;

/** Appel REST synchrone vers consumption-service (RG-08). */
@Component
public class ConsumptionClient {

    private final RestClient consumptionRestClient;

    public ConsumptionClient(@Qualifier("consumptionRestClient") RestClient consumptionRestClient) {
        this.consumptionRestClient = consumptionRestClient;
    }

    public long countByPedestal(Long pedestalId) {
        try {
            ConsumptionSummaryResponse summary = consumptionRestClient.get()
                    .uri("/api/consumptions/summary?pedestalId={id}", pedestalId)
                    .retrieve()
                    .body(ConsumptionSummaryResponse.class);
            return summary == null ? 0 : summary.count();
        } catch (RestClientException exception) {
            throw new ConsumptionServiceUnavailableException();
        }
    }
}
