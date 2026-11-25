package tn.iset.marina.consumption_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import tn.iset.marina.consumption_service.entity.Consumption;
import tn.iset.marina.consumption_service.entity.ConsumptionType;

import java.math.BigDecimal;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long>, JpaSpecificationExecutor<Consumption> {

    long countByPedestalId(Long pedestalId);

    @Query("select coalesce(sum(c.quantity), 0) from Consumption c where c.pedestalId = :pedestalId and c.type = :type")
    BigDecimal sumQuantity(Long pedestalId, ConsumptionType type);
}
