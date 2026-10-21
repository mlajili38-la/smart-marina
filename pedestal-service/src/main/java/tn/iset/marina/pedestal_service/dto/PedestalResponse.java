package tn.iset.marina.pedestal_service.dto;

import tn.iset.marina.pedestal_service.entity.PedestalStatus;

import java.time.Instant;

public record PedestalResponse(
        Long id,
        String serialNumber,
        String name,
        Long portId,
        String pontoon,
        PedestalStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
