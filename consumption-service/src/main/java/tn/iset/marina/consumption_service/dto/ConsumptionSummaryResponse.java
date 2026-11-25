package tn.iset.marina.consumption_service.dto;

import java.math.BigDecimal;

/** Bilan d'une borne : utilisé par pedestal-service pour la règle RG-08 et par les tableaux de bord. */
public record ConsumptionSummaryResponse(
        Long pedestalId,
        long count,
        BigDecimal totalElectricityKwh,
        BigDecimal totalWaterM3
) {
}
