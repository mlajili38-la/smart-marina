package tn.iset.marina.pedestal_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.marina.pedestal_service.client.ConsumptionClient;
import tn.iset.marina.pedestal_service.client.PortClient;
import tn.iset.marina.pedestal_service.dto.PedestalPageResponse;
import tn.iset.marina.pedestal_service.dto.PedestalRequest;
import tn.iset.marina.pedestal_service.dto.PedestalResponse;
import tn.iset.marina.pedestal_service.entity.Pedestal;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;
import tn.iset.marina.pedestal_service.exception.DuplicateSerialNumberException;
import tn.iset.marina.pedestal_service.exception.ForbiddenPortAccessException;
import tn.iset.marina.pedestal_service.exception.PedestalInUseException;
import tn.iset.marina.pedestal_service.exception.PedestalNotFoundException;
import tn.iset.marina.pedestal_service.repository.PedestalRepository;
import tn.iset.marina.pedestal_service.security.CurrentUser;

@Service
@RequiredArgsConstructor
public class PedestalService {

    private final PedestalRepository repository;
    private final PortClient portClient;
    private final ConsumptionClient consumptionClient;

    @Transactional(readOnly = true)
    public PedestalPageResponse findAll(Pageable pageable, Long portId, PedestalStatus status) {
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
    public PedestalResponse create(PedestalRequest request, CurrentUser user) {
        checkCanManage(user, request.portId());
        portClient.getPort(request.portId());
        if (repository.existsBySerialNumber(request.serialNumber())) {
            throw new DuplicateSerialNumberException(request.serialNumber());
        }

        Pedestal pedestal = new Pedestal();
        apply(pedestal, request);
        return save(pedestal);
    }

    @Transactional
    public PedestalResponse update(Long id, PedestalRequest request, CurrentUser user) {
        Pedestal pedestal = getOrThrow(id);
        // RG-09 : le gestionnaire doit gérer le port actuel ET le port cible (pas de transfert vers un autre port)
        checkCanManage(user, pedestal.getPortId());
        checkCanManage(user, request.portId());
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
    public void delete(Long id, CurrentUser user) {
        Pedestal pedestal = getOrThrow(id);
        checkCanManage(user, pedestal.getPortId());
        // RG-08 : une borne ayant déjà des consommations ne peut pas être supprimée (historique à conserver)
        long consumptions = consumptionClient.countByPedestal(id);
        if (consumptions > 0) {
            throw new PedestalInUseException(id, consumptions);
        }
        repository.delete(pedestal);
    }

    private void checkCanManage(CurrentUser user, Long portId) {
        if (!user.canManagePort(portId)) {
            throw new ForbiddenPortAccessException(portId);
        }
    }

    private Pedestal getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new PedestalNotFoundException(id));
    }

    private PedestalResponse save(Pedestal pedestal) {
        try {
            // saveAndFlush : la contrainte d'unicité est vérifiée ici, et non au commit
            return toResponse(repository.saveAndFlush(pedestal));
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
