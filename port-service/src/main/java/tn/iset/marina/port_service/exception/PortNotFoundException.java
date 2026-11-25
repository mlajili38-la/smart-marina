package tn.iset.marina.port_service.exception;

public class PortNotFoundException extends RuntimeException {

    public PortNotFoundException(Long id) {
        super("Port introuvable : " + id);
    }
}
