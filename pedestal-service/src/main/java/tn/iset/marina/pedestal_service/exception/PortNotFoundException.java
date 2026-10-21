package tn.iset.marina.pedestal_service.exception;

public class PortNotFoundException extends RuntimeException {

    public PortNotFoundException(Long id) {
        super("Port introuvable : " + id);
    }
}
