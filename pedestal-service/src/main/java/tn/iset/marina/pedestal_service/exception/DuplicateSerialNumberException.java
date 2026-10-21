package tn.iset.marina.pedestal_service.exception;

public class DuplicateSerialNumberException extends RuntimeException {

    public DuplicateSerialNumberException(String serialNumber) {
        super("Le numéro de série existe déjà : " + serialNumber);
    }
}
