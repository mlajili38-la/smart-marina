package tn.iset.marina.port_service.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tn.iset.marina.port_service.exception.PedestalServiceUnavailableException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Vrai appel HTTP vers un faux pedestal-service local : vérifie la lecture JSON, les erreurs et le timeout. */
class PedestalClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void readsTotalElementsFromPedestalPage() throws IOException {
        // Page complète telle que renvoyée par pedestal-service : les champs inconnus sont ignorés
        start(200, "{\"content\":[{\"id\":4,\"serialNumber\":\"XYZ-PED-004\"}],"
                + "\"page\":0,\"size\":1,\"totalElements\":3,\"totalPages\":3}", 0);

        assertEquals(3, client(Duration.ofSeconds(3)).countByPort(7L));
        assertEquals("portId=7&size=1", lastQuery.get());
    }

    @Test
    void serverErrorBecomesUnavailable() throws IOException {
        start(500, "{}", 0);

        assertThrows(PedestalServiceUnavailableException.class, () -> client(Duration.ofSeconds(3)).countByPort(7L));
    }

    @Test
    void timeoutBecomesUnavailable() throws IOException {
        start(200, "{\"totalElements\":0}", 1500);

        assertThrows(PedestalServiceUnavailableException.class,
                () -> client(Duration.ofMillis(300)).countByPort(7L));
    }

    @Test
    void unreachableServiceBecomesUnavailable() {
        RestClient restClient = RestClient.builder().baseUrl("http://localhost:1").build();

        assertThrows(PedestalServiceUnavailableException.class, () -> new PedestalClient(restClient).countByPort(7L));
    }

    private PedestalClient client(Duration readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(readTimeout);
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .requestFactory(factory)
                .build();
        return new PedestalClient(restClient);
    }

    private void start(int status, String body, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/pedestals", exchange -> {
            lastQuery.set(exchange.getRequestURI().getQuery());
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            } catch (IOException ignored) {
                // le client a abandonné (timeout)
            }
        });
        server.start();
    }
}
