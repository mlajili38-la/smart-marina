package tn.iset.marina.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Sécurité centralisée : la Gateway vérifie le jeton JWT émis par Keycloak
 * et applique les règles d'accès par rôle (ADMIN, PORT_MANAGER, BOATER).
 * Les règles fines (ex. RG-09 : un gestionnaire ne gère que son port) restent dans les services.
 */
@Configuration
public class SecurityConfig {

    public static final String ADMIN = "ADMIN";
    public static final String PORT_MANAGER = "PORT_MANAGER";

    @Bean
    @ConditionalOnProperty(name = "smart-marina.security.enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain securedChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/api/ports/**").hasRole(ADMIN)
                        .requestMatchers("/api/pedestals/**").hasAnyRole(ADMIN, PORT_MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/consumptions/**").hasAnyRole(ADMIN, PORT_MANAGER)
                        .requestMatchers("/api/consumptions/**").hasRole(ADMIN)
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/actuator/**").hasRole(ADMIN)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakConverter())));
        return http.build();
    }

    /** Mode développement : sans Keycloak, tout est ouvert (smart-marina.security.enabled=false). */
    @Bean
    @ConditionalOnProperty(name = "smart-marina.security.enabled", havingValue = "false")
    SecurityFilterChain openChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Keycloak place les rôles du realm dans la claim realm_access.roles. */
    static Converter<Jwt, AbstractAuthenticationToken> keycloakConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, realmRoles(jwt), jwt.getClaimAsString("preferred_username"));
    }

    @SuppressWarnings("unchecked")
    static Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return ((Collection<Object>) roles).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${smart-marina.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
