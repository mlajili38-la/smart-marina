package tn.iset.marina.consumption_service.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import tn.iset.marina.consumption_service.entity.Consumption;
import tn.iset.marina.consumption_service.entity.ConsumptionType;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Base H2 en mémoire : vérifie les requêtes réelles (filtres, sommes) sans PostgreSQL. */
@DataJpaTest
class ConsumptionRepositoryTest {

    @Autowired
    private ConsumptionRepository repository;

    @Test
    void filtersAndAggregates() {
        repository.save(consumption(5L, 3L, ConsumptionType.ELECTRICITY, "10.500"));
        repository.save(consumption(5L, 3L, ConsumptionType.ELECTRICITY, "4.000"));
        repository.save(consumption(5L, 3L, ConsumptionType.WATER, "0.250"));
        repository.save(consumption(8L, 7L, ConsumptionType.WATER, "1.000"));

        assertEquals(3, repository.countByPedestalId(5L));
        assertEquals(0, new BigDecimal("14.500").compareTo(repository.sumQuantity(5L, ConsumptionType.ELECTRICITY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(repository.sumQuantity(99L, ConsumptionType.WATER)));

        var water = repository.findAll(ConsumptionSpecifications.filter(null, null, ConsumptionType.WATER),
                PageRequest.of(0, 10));
        assertEquals(2, water.getTotalElements());

        var port7 = repository.findAll(ConsumptionSpecifications.filter(null, 7L, null), PageRequest.of(0, 10));
        assertEquals(1, port7.getTotalElements());
    }

    private Consumption consumption(Long pedestalId, Long portId, ConsumptionType type, String quantity) {
        Consumption consumption = new Consumption();
        consumption.setPedestalId(pedestalId);
        consumption.setPortId(portId);
        consumption.setType(type);
        consumption.setQuantity(new BigDecimal(quantity));
        consumption.setStartedAt(Instant.parse("2026-11-10T10:00:00Z"));
        consumption.setEndedAt(Instant.parse("2026-11-10T12:00:00Z"));
        return consumption;
    }
}
