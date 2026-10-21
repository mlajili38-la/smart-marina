package tn.iset.marina.pedestal_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.marina.pedestal_service.entity.Pedestal;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;

import java.util.Optional;

public interface PedestalRepository extends JpaRepository<Pedestal, Long> {

    Optional<Pedestal> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);

    Page<Pedestal> findAllByPortId(Long portId, Pageable pageable);

    Page<Pedestal> findAllByStatus(PedestalStatus status, Pageable pageable);

    Page<Pedestal> findAllByPortIdAndStatus(Long portId, PedestalStatus status, Pageable pageable);
}
