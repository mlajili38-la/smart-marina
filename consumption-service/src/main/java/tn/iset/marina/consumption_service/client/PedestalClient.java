package tn.iset.marina.consumption_service.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.iset.marina.consumption_service.dto.PedestalResponse;
import tn.iset.marina.consumption_service.exception.PedestalNotFoundException;
import tn.iset.marina.consumption_service.exception.PedestalServiceUnavailableException;

/** Appel REST synchrone : on a besoin de la réponse tout de suite pour valider la consommation. */
@Component
public class PedestalClient {

    private final RestClient pedestalRestClient;

    public PedestalClient(RestClient pedestalRestClient) {
        this.pedestalRestClient = pedestalRestClient;
    }

    public PedestalResponse getPedestal(Long id) {
        try {
            return pedestalRestClient.get()
                    .uri("/api/pedestals/{id}", id)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new PedestalNotFoundException(id);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new PedestalServiceUnavailableException();
                    })
                    .body(PedestalResponse.class);
        } catch (PedestalNotFoundException | PedestalServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new PedestalServiceUnavailableException();
        }
    }
}
