package tn.iset.marina.consumption_service.dto;

import tn.iset.marina.consumption_service.entity.ConsumptionType;

import java.math.BigDecimal;
import java.time.Instant;

public record ConsumptionResponse(
        Long id,
        Long pedestalId,
        Long portId,
        ConsumptionType type,
        BigDecimal quantity,
        String unit,
        String boatName,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt
) {
}
