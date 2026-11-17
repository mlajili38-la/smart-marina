package tn.iset.marina.consumption_service.repository;

import org.springframework.data.jpa.domain.Specification;
import tn.iset.marina.consumption_service.entity.Consumption;
import tn.iset.marina.consumption_service.entity.ConsumptionType;

/** Filtres optionnels combinables pour la recherche des consommations. */
public final class ConsumptionSpecifications {

    private ConsumptionSpecifications() {
    }

    public static Specification<Consumption> filter(Long pedestalId, Long portId, ConsumptionType type) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (pedestalId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("pedestalId"), pedestalId));
            }
            if (portId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("portId"), portId));
            }
            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }
            return predicate;
        };
    }
}
