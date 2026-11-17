package tn.iset.marina.notification_service.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long id) {
        super("Notification introuvable : " + id);
    }
}
