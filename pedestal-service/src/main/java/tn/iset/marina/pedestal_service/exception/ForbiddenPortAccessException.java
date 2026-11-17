package tn.iset.marina.pedestal_service.exception;

public class ForbiddenPortAccessException extends RuntimeException {

    public ForbiddenPortAccessException(Long portId) {
        super("Vous n'êtes pas autorisé à gérer les bornes du port " + portId);
    }
}
