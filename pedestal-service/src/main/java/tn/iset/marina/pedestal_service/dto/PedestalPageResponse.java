package tn.iset.marina.pedestal_service.dto;

import java.util.List;

public record PedestalPageResponse(
        List<PedestalResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
