package tn.iset.marina.consumption_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.marina.consumption_service.client.PedestalClient;
import tn.iset.marina.consumption_service.dto.ConsumptionPageResponse;
import tn.iset.marina.consumption_service.dto.ConsumptionRequest;
import tn.iset.marina.consumption_service.dto.ConsumptionResponse;
import tn.iset.marina.consumption_service.dto.ConsumptionSummaryResponse;
import tn.iset.marina.consumption_service.dto.PedestalResponse;
import tn.iset.marina.consumption_service.entity.Consumption;
import tn.iset.marina.consumption_service.entity.ConsumptionType;
import tn.iset.marina.consumption_service.event.ConsumptionRecordedEvent;
import tn.iset.marina.consumption_service.exception.ConsumptionNotFoundException;
import tn.iset.marina.consumption_service.exception.ForbiddenPortAccessException;
import tn.iset.marina.consumption_service.exception.PedestalNotActiveException;
import tn.iset.marina.consumption_service.repository.ConsumptionRepository;
import tn.iset.marina.consumption_service.repository.ConsumptionSpecifications;
import tn.iset.marina.consumption_service.security.CurrentUser;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsumptionService {

    private final ConsumptionRepository repository;
    private final PedestalClient pedestalClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ConsumptionResponse record(ConsumptionRequest request, CurrentUser user) {
        // 1. Vérification synchrone (REST) : la borne existe et elle est en service
        PedestalResponse pedestal = pedestalClient.getPedestal(request.pedestalId());
        if (!pedestal.isActive()) {
            throw new PedestalNotActiveException(pedestal.id(), pedestal.status());
        }
        // 2. RG-09 : un gestionnaire n'enregistre que sur les bornes de son port
        if (!user.canManagePort(pedestal.portId())) {
            throw new ForbiddenPortAccessException(pedestal.portId());
        }

        Consumption consumption = new Consumption();
        consumption.setPedestalId(pedestal.id());
        consumption.setPortId(pedestal.portId());
        consumption.setType(request.type());
        consumption.setQuantity(request.quantity());
        consumption.setBoatName(request.boatName());
        consumption.setStartedAt(request.startedAt());
        consumption.setEndedAt(request.endedAt());
        consumption.setRecordedBy(user.id());
        Consumption saved = repository.save(consumption);

        // 3. Événement asynchrone (Kafka) : envoyé par ConsumptionEventPublisher après le commit
        eventPublisher.publishEvent(toEvent(saved, pedestal));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ConsumptionPageResponse findAll(Pageable pageable, Long pedestalId, Long portId,
                                           ConsumptionType type, CurrentUser user) {
        Long effectivePortId = portId;
        if (user.isRestrictedToOwnPort()) {
            if (portId != null && !portId.equals(user.portId())) {
                throw new ForbiddenPortAccessException(portId);
            }
            effectivePortId = user.portId();
        }
        Page<Consumption> page = repository.findAll(
                ConsumptionSpecifications.filter(pedestalId, effectivePortId, type), pageable);
        return new ConsumptionPageResponse(page.map(this::toResponse).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ConsumptionResponse findById(Long id, CurrentUser user) {
        Consumption consumption = getOrThrow(id);
        if (user.isRestrictedToOwnPort() && !user.canManagePort(consumption.getPortId())) {
            throw new ForbiddenPortAccessException(consumption.getPortId());
        }
        return toResponse(consumption);
    }

    @Transactional(readOnly = true)
    public ConsumptionSummaryResponse summary(Long pedestalId) {
        return new ConsumptionSummaryResponse(
                pedestalId,
                repository.countByPedestalId(pedestalId),
                repository.sumQuantity(pedestalId, ConsumptionType.ELECTRICITY),
                repository.sumQuantity(pedestalId, ConsumptionType.WATER));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Consumption getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ConsumptionNotFoundException(id));
    }

    private ConsumptionRecordedEvent toEvent(Consumption consumption, PedestalResponse pedestal) {
        return new ConsumptionRecordedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                consumption.getId(),
                consumption.getPedestalId(),
                pedestal.serialNumber(),
                consumption.getPortId(),
                consumption.getType().name(),
                consumption.getQuantity(),
                consumption.getType().unit(),
                consumption.getBoatName());
    }

    private ConsumptionResponse toResponse(Consumption consumption) {
        return new ConsumptionResponse(
                consumption.getId(),
                consumption.getPedestalId(),
                consumption.getPortId(),
                consumption.getType(),
                consumption.getQuantity(),
                consumption.getType().unit(),
                consumption.getBoatName(),
                consumption.getStartedAt(),
                consumption.getEndedAt(),
                consumption.getCreatedAt());
    }
}
