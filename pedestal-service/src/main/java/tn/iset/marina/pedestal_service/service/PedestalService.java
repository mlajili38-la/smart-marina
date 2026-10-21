package tn.iset.marina.pedestal_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.marina.pedestal_service.client.PortClient;
import tn.iset.marina.pedestal_service.dto.PedestalPageResponse;
import tn.iset.marina.pedestal_service.dto.PedestalRequest;
import tn.iset.marina.pedestal_service.dto.PedestalResponse;
import tn.iset.marina.pedestal_service.entity.Pedestal;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;
import tn.iset.marina.pedestal_service.exception.DuplicateSerialNumberException;
import tn.iset.marina.pedestal_service.exception.PedestalNotFoundException;
import tn.iset.marina.pedestal_service.repository.PedestalRepository;

@Service
@RequiredArgsConstructor
// TODO RG-09 : la sécurité par port sera ajoutée en semaine 8 avec JWT/Keycloak.
public class PedestalService {

    private final PedestalRepository repository;
    private final PortClient portClient;

    @Transactional(readOnly = true)
    public PedestalPageResponse findAll(int page, int size, Sort sort, Long portId, PedestalStatus status) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Pedestal> pedestals;
        if (portId != null && status != null) {
            pedestals = repository.findAllByPortIdAndStatus(portId, status, pageable);
        } else if (portId != null) {
            pedestals = repository.findAllByPortId(portId, pageable);
        } else if (status != null) {
            pedestals = repository.findAllByStatus(status, pageable);
        } else {
            pedestals = repository.findAll(pageable);
        }
        return new PedestalPageResponse(
                pedestals.map(this::toResponse).getContent(),
                pedestals.getNumber(),
                pedestals.getSize(),
                pedestals.getTotalElements(),
                pedestals.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PedestalResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public PedestalResponse create(PedestalRequest request) {
        portClient.getPort(request.portId());
        if (repository.existsBySerialNumber(request.serialNumber())) {
            throw new DuplicateSerialNumberException(request.serialNumber());
        }

        Pedestal pedestal = new Pedestal();
        apply(pedestal, request);
        return save(pedestal);
    }

    @Transactional
    public PedestalResponse update(Long id, PedestalRequest request) {
        Pedestal pedestal = getOrThrow(id);
        portClient.getPort(request.portId());
        repository.findBySerialNumber(request.serialNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateSerialNumberException(request.serialNumber());
                });
        apply(pedestal, request);
        return save(pedestal);
    }

    @Transactional
    public void delete(Long id) {
        Pedestal pedestal = getOrThrow(id);
        // TODO RG-08 : refuser la suppression si des réservations ou consommations existent.
        repository.delete(pedestal);
    }

    private Pedestal getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new PedestalNotFoundException(id));
    }

    private PedestalResponse save(Pedestal pedestal) {
        try {
            return toResponse(repository.save(pedestal));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSerialNumberException(pedestal.getSerialNumber());
        }
    }

    private void apply(Pedestal pedestal, PedestalRequest request) {
        pedestal.setSerialNumber(request.serialNumber());
        pedestal.setName(request.name());
        pedestal.setPortId(request.portId());
        pedestal.setPontoon(request.pontoon());
        pedestal.setStatus(request.status());
    }

    private PedestalResponse toResponse(Pedestal pedestal) {
        return new PedestalResponse(
                pedestal.getId(),
                pedestal.getSerialNumber(),
                pedestal.getName(),
                pedestal.getPortId(),
                pedestal.getPontoon(),
                pedestal.getStatus(),
                pedestal.getCreatedAt(),
                pedestal.getUpdatedAt()
        );
    }
}
