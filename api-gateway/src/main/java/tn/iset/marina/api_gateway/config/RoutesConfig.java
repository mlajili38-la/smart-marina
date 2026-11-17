package tn.iset.marina.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

/**
 * Routage : chaque préfixe d'URL est redirigé vers le microservice propriétaire.
 * La Gateway ne contient aucune logique métier.
 */
@Configuration
public class RoutesConfig {

    @Bean
    RouterFunction<ServerResponse> portRoutes(@Value("${services.port-service}") String url) {
        return proxy("port-service", "/api/ports/**", url);
    }

    @Bean
    RouterFunction<ServerResponse> pedestalRoutes(@Value("${services.pedestal-service}") String url) {
        return proxy("pedestal-service", "/api/pedestals/**", url);
    }

    @Bean
    RouterFunction<ServerResponse> consumptionRoutes(@Value("${services.consumption-service}") String url) {
        return proxy("consumption-service", "/api/consumptions/**", url);
    }

    @Bean
    RouterFunction<ServerResponse> notificationRoutes(@Value("${services.notification-service}") String url) {
        return proxy("notification-service", "/api/notifications/**", url);
    }

    private RouterFunction<ServerResponse> proxy(String id, String pattern, String url) {
        return route(id)
                .route(path(pattern), http())
                .before(uri(url))
                .before(IdentityHeaders::propagate)
                .build();
    }
}
