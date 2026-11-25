package tn.iset.marina.pedestal_service.exception;

public class PedestalInUseException extends RuntimeException {

    public PedestalInUseException(Long id, long consumptions) {
        super("Suppression impossible : la borne " + id + " possède " + consumptions + " consommation(s)");
    }
}
