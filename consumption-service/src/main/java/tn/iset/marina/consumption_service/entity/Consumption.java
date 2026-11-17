package tn.iset.marina.consumption_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une consommation mesurée sur une borne pendant une période.
 * pedestalId et portId sont des références distantes (aucune relation JPA vers les autres services) :
 * portId est copié au moment de l'enregistrement pour filtrer par port sans appel réseau.
 */
@Entity
@Table(name = "consumptions", indexes = {
        @Index(name = "idx_consumption_pedestal", columnList = "pedestal_id"),
        @Index(name = "idx_consumption_port", columnList = "port_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Consumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedestal_id", nullable = false)
    private Long pedestalId;

    @Column(name = "port_id", nullable = false)
    private Long portId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsumptionType type;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 100)
    private String boatName;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endedAt;

    @Column(length = 100)
    private String recordedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
