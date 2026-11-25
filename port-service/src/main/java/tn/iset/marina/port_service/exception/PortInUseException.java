package tn.iset.marina.port_service.exception;

public class PortInUseException extends RuntimeException {

    public PortInUseException(Long id, long pedestals) {
        super("Suppression impossible : le port " + id + " possède encore " + pedestals + " borne(s)");
    }
}
