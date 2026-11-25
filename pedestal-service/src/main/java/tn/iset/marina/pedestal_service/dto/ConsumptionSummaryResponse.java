package tn.iset.marina.pedestal_service.dto;

/** Réponse de GET /api/consumptions/summary?pedestalId=... (consumption-service). */
public record ConsumptionSummaryResponse(Long pedestalId, long count) {
}
