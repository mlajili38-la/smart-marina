package tn.iset.marina.port_service.dto;

public record PortResponse(
        Long id,
        String name,
        String city,
        String country,
        Integer capacity
) {}