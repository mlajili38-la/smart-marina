package tn.iset.marina.pedestal_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.iset.marina.pedestal_service.dto.PortResponse;
import tn.iset.marina.pedestal_service.exception.PortNotFoundException;
import tn.iset.marina.pedestal_service.exception.PortServiceUnavailableException;

@Component
@RequiredArgsConstructor
public class PortClient {

    private final RestClient portRestClient;

    public PortResponse getPort(Long id) {
        try {
            return portRestClient.get()
                    .uri("/api/ports/{id}", id)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> {
                                throw new PortNotFoundException(id);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (request, response) -> {
                                throw new PortServiceUnavailableException();
                            })
                    .body(PortResponse.class);
        } catch (PortNotFoundException | PortServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new PortServiceUnavailableException();
        }
    }
}
