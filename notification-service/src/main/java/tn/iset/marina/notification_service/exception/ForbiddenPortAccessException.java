package tn.iset.marina.notification_service.exception;

public class ForbiddenPortAccessException extends RuntimeException {

    public ForbiddenPortAccessException(Long portId) {
        super("Vous n'êtes pas autorisé à consulter les notifications du port " + portId);
    }
}
