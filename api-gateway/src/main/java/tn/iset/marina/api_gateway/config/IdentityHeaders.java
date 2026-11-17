package tn.iset.marina.api_gateway.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.stream.Collectors;

/**
 * Transmet l'identité de l'utilisateur aux microservices sous forme d'en-têtes.
 * Les en-têtes envoyés par le client sont toujours supprimés : seul le jeton validé
 * par la Gateway fait foi. Aucune règle métier ici, uniquement de la propagation.
 */
public final class IdentityHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String USER_PORT_ID = "X-User-Port-Id";

    private IdentityHeaders() {
    }

    public static ServerRequest propagate(ServerRequest request) {
        ServerRequest.Builder builder = ServerRequest.from(request).headers(headers -> {
            headers.remove(USER_ID);
            headers.remove(USER_ROLES);
            headers.remove(USER_PORT_ID);
        });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            String roles = token.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .collect(Collectors.joining(","));
            builder.header(USER_ID, token.getToken().getSubject());
            builder.header(USER_ROLES, roles);
            Object portId = token.getToken().getClaims().get("port_id");
            if (portId != null) {
                builder.header(USER_PORT_ID, portId.toString());
            }
        }
        return builder.build();
    }
}
