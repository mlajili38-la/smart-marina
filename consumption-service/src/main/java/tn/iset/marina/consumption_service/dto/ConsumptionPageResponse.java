package tn.iset.marina.consumption_service.dto;

import java.util.List;

public record ConsumptionPageResponse(
        List<ConsumptionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
