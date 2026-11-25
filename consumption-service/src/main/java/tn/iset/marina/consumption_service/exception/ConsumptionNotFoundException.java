package tn.iset.marina.consumption_service.exception;

public class ConsumptionNotFoundException extends RuntimeException {

    public ConsumptionNotFoundException(Long id) {
        super("Consommation introuvable : " + id);
    }
}
