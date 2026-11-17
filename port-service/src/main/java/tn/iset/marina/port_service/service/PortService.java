package tn.iset.marina.port_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.marina.port_service.dto.PortRequest;
import tn.iset.marina.port_service.dto.PortResponse;
import tn.iset.marina.port_service.entity.Port;
import tn.iset.marina.port_service.exception.PortNotFoundException;
import tn.iset.marina.port_service.repository.PortRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortService {

    private final PortRepository repository;

    @Transactional(readOnly = true)
    public List<PortResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PortResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public PortResponse create(PortRequest request) {
        Port port = new Port();
        apply(port, request);
        return toResponse(repository.save(port));
    }

    @Transactional
    public PortResponse update(Long id, PortRequest request) {
        Port port = getOrThrow(id);
        apply(port, request);
        return toResponse(repository.save(port));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Port getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new PortNotFoundException(id));
    }

    private void apply(Port port, PortRequest request) {
        port.setName(request.name());
        port.setCity(request.city());
        port.setCountry(request.country());
        port.setCapacity(request.capacity());
    }

    private PortResponse toResponse(Port port) {
        return new PortResponse(port.getId(), port.getName(), port.getCity(),
                port.getCountry(), port.getCapacity());
    }
}
