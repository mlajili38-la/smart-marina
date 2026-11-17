package tn.iset.marina.port_service.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.iset.marina.port_service.dto.PedestalCountResponse;
import tn.iset.marina.port_service.exception.PedestalServiceUnavailableException;

/** Appel REST synchrone vers pedestal-service : combien de bornes sont rattachées à ce port ? */
@Component
public class PedestalClient {

    private final RestClient pedestalRestClient;

    public PedestalClient(RestClient pedestalRestClient) {
        this.pedestalRestClient = pedestalRestClient;
    }

    public long countByPort(Long portId) {
        try {
            PedestalCountResponse page = pedestalRestClient.get()
                    .uri("/api/pedestals?portId={id}&size=1", portId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new PedestalServiceUnavailableException();
                    })
                    .body(PedestalCountResponse.class);
            return page == null ? 0 : page.totalElements();
        } catch (PedestalServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new PedestalServiceUnavailableException();
        }
    }
}
