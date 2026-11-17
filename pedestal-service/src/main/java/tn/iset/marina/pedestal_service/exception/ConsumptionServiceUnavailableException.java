package tn.iset.marina.pedestal_service.exception;

public class ConsumptionServiceUnavailableException extends RuntimeException {

    public ConsumptionServiceUnavailableException() {
        super("Le service des consommations est indisponible");
    }
}
