package tn.iset.marina.port_service.dto;

/**
 * Vue locale de la page renvoyée par GET /api/pedestals?portId=...&size=1 (pedestal-service) :
 * seul le nombre total de bornes nous intéresse, les autres champs sont ignorés.
 */
public record PedestalCountResponse(long totalElements) {
}
