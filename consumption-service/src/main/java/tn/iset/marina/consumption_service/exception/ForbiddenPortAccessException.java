package tn.iset.marina.consumption_service.exception;

public class ForbiddenPortAccessException extends RuntimeException {

    public ForbiddenPortAccessException(Long portId) {
        super("Vous n'êtes pas autorisé à gérer les consommations du port " + portId);
    }
}
