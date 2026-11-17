package tn.iset.marina.consumption_service.dto;

/** Vue locale de la borne renvoyée par pedestal-service (seuls les champs utiles). */
public record PedestalResponse(Long id, String serialNumber, String name, Long portId, String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
