package tn.iset.marina.api_gateway;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayRoutingTest {

    private static final HttpServer backend = startBackend();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<Headers> lastHeaders = new AtomicReference<>();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void backendUrls(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + backend.getAddress().getPort();
        registry.add("services.port-service", () -> url);
        registry.add("services.pedestal-service", () -> url);
        registry.add("services.consumption-service", () -> url);
        registry.add("services.notification-service", () -> url);
    }

    @BeforeEach
    void reset() {
        lastPath.set(null);
        lastHeaders.set(null);
    }

    @AfterAll
    static void stopBackend() {
        backend.stop(0);
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/ports"))
                .andExpect(status().isUnauthorized());
        assertNull(lastPath.get());
    }

    @Test
    void routesAuthenticatedRequestAndPropagatesIdentity() throws Exception {
        mockMvc.perform(get("/api/pedestals/5")
                        .with(jwt().jwt(token -> token.subject("user-1").claim("port_id", 3))
                                .authorities(new SimpleGrantedAuthority("ROLE_PORT_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"ok\":true}"));

        assertEquals("/api/pedestals/5", lastPath.get());
        assertEquals("user-1", lastHeaders.get().getFirst("X-User-Id"));
        assertEquals("PORT_MANAGER", lastHeaders.get().getFirst("X-User-Roles"));
        assertEquals("3", lastHeaders.get().getFirst("X-User-Port-Id"));
    }

    @Test
    void stripsIdentityHeadersSentByClient() throws Exception {
        mockMvc.perform(get("/api/consumptions")
                        .header("X-User-Roles", "ADMIN")
                        .header("X-User-Port-Id", "99")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BOATER"))))
                .andExpect(status().isOk());

        assertEquals("BOATER", lastHeaders.get().getFirst("X-User-Roles"));
        assertNull(lastHeaders.get().getFirst("X-User-Port-Id"));
    }

    @Test
    void forbidsPortCreationForPortManager() throws Exception {
        mockMvc.perform(post("/api/ports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mahdia\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PORT_MANAGER"))))
                .andExpect(status().isForbidden());
        assertNull(lastPath.get());
    }

    @Test
    void allowsPortCreationForAdmin() throws Exception {
        mockMvc.perform(post("/api/ports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mahdia\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        assertEquals("/api/ports", lastPath.get());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private static HttpServer startBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                lastPath.set(exchange.getRequestURI().getPath());
                lastHeaders.set(exchange.getRequestHeaders());
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
