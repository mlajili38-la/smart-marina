package tn.iset.marina.port_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.marina.port_service.entity.Port;

public interface PortRepository extends JpaRepository<Port, Long> {
}