package tn.iset.marina.consumption_service.exception;

public class PedestalServiceUnavailableException extends RuntimeException {

    public PedestalServiceUnavailableException() {
        super("Le service des bornes est indisponible");
    }
}
