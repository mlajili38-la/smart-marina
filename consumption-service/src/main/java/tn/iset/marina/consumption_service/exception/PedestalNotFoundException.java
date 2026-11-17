package tn.iset.marina.consumption_service.exception;

public class PedestalNotFoundException extends RuntimeException {

    public PedestalNotFoundException(Long id) {
        super("Borne introuvable : " + id);
    }
}
