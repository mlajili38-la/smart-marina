package tn.iset.marina.notification_service.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Identité transmise par l'API Gateway après validation du jeton JWT
 * (en-têtes X-User-Id, X-User-Roles, X-User-Port-Id).
 * Sans en-tête de rôle (appel interne ou sécurité désactivée), aucune restriction n'est appliquée.
 */
public record CurrentUser(String id, Set<String> roles, Long portId) {

    public static final String ADMIN = "ADMIN";
    public static final String PORT_MANAGER = "PORT_MANAGER";

    public static final CurrentUser SYSTEM = new CurrentUser(null, Set.of(), null);

    public static CurrentUser of(String id, String roles, String portId) {
        Set<String> roleSet = roles == null ? Set.of() : Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        Long port = null;
        if (portId != null && !portId.isBlank()) {
            try {
                port = Long.valueOf(portId.trim());
            } catch (NumberFormatException ignored) {
                port = null;
            }
        }
        return new CurrentUser(id, roleSet, port);
    }

    public boolean isAuthenticated() {
        return !roles.isEmpty();
    }

    /** Un gestionnaire de port (non ADMIN) ne voit et ne gère que son port. */
    public boolean isRestrictedToOwnPort() {
        return isAuthenticated() && !roles.contains(ADMIN) && roles.contains(PORT_MANAGER);
    }

    /** RG-09 : un ADMIN gère tous les ports, un PORT_MANAGER uniquement le sien. */
    public boolean canManagePort(Long targetPortId) {
        if (!isAuthenticated() || roles.contains(ADMIN)) {
            return true;
        }
        return roles.contains(PORT_MANAGER) && portId != null && portId.equals(targetPortId);
    }
}
