package tn.iset.marina.port_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PortRequest(
        @NotBlank String name,
        String city,
        String country,
        @Positive Integer capacity
) {}