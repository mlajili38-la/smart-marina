package tn.iset.marina.consumption_service.exception;

public class PedestalNotActiveException extends RuntimeException {

    public PedestalNotActiveException(Long id, String status) {
        super("La borne " + id + " n'est pas active (statut : " + status + ")");
    }
}
