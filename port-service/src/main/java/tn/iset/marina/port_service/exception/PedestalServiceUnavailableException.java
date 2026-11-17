package tn.iset.marina.port_service.exception;

public class PedestalServiceUnavailableException extends RuntimeException {

    public PedestalServiceUnavailableException() {
        super("Le service des bornes est indisponible");
    }
}
