package tn.iset.marina.pedestal_service.exception;

public class PortServiceUnavailableException extends RuntimeException {

    public PortServiceUnavailableException() {
        super("Le service des ports est indisponible");
    }
}
