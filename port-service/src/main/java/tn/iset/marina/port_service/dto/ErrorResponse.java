package tn.iset.marina.port_service.dto;

import java.time.Instant;
import java.util.List;

/** Format d'erreur commun à tous les microservices Smart Marina. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
