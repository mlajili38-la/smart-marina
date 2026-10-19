package tn.iset.marina.port_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Port {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String city;
    private String country;
    private Integer capacity;
}