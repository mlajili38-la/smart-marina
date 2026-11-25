package tn.iset.marina.port_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PortRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String city,
        @Size(max = 100) String country,
        @Positive Integer capacity
) {}
