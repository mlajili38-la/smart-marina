package tn.iset.marina.pedestal_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iset.marina.pedestal_service.client.PortClient;
import tn.iset.marina.pedestal_service.dto.PedestalRequest;
import tn.iset.marina.pedestal_service.dto.PortResponse;
import tn.iset.marina.pedestal_service.entity.Pedestal;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;
import tn.iset.marina.pedestal_service.exception.DuplicateSerialNumberException;
import tn.iset.marina.pedestal_service.exception.PedestalNotFoundException;
import tn.iset.marina.pedestal_service.exception.PortNotFoundException;
import tn.iset.marina.pedestal_service.exception.PortServiceUnavailableException;
import tn.iset.marina.pedestal_service.repository.PedestalRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedestalServiceTest {

    @Mock
    private PedestalRepository repository;

    @Mock
    private PortClient portClient;

    private PedestalService service;

    @BeforeEach
    void setUp() {
        service = new PedestalService(repository, portClient);
    }

    @Test
    void createSavesPedestalAfterCheckingPort() {
        when(portClient.getPort(3L)).thenReturn(new PortResponse(3L, "Mahdia", "Mahdia", "Tunisie", 200));
        when(repository.save(any(Pedestal.class))).thenAnswer(invocation -> {
            Pedestal saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(saved.getCreatedAt());
            return saved;
        });

        var response = service.create(request("XYZ-PED-001", 3L));

        assertEquals(1L, response.id());
        assertEquals("XYZ-PED-001", response.serialNumber());
        verify(portClient).getPort(3L);
    }

    @Test
    void createRejectsDuplicateSerialNumber() {
        when(portClient.getPort(3L)).thenReturn(new PortResponse(3L, "Mahdia", "Mahdia", "Tunisie", 200));
        when(repository.existsBySerialNumber("XYZ-PED-001")).thenReturn(true);

        assertThrows(DuplicateSerialNumberException.class,
                () -> service.create(request("XYZ-PED-001", 3L)));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownPort() {
        when(portClient.getPort(999L)).thenThrow(new PortNotFoundException(999L));

        assertThrows(PortNotFoundException.class, () -> service.create(request("XYZ-PED-001", 999L)));
        verify(repository, never()).save(any());
    }

    @Test
    void createReportsUnavailablePortService() {
        when(portClient.getPort(3L)).thenThrow(new PortServiceUnavailableException());

        assertThrows(PortServiceUnavailableException.class, () -> service.create(request("XYZ-PED-001", 3L)));
        verify(repository, never()).save(any());
    }

    @Test
    void createMapsConcurrentUniqueConstraintViolationToConflict() {
        when(portClient.getPort(3L)).thenReturn(new PortResponse(3L, "Mahdia", "Mahdia", "Tunisie", 200));
        when(repository.save(any(Pedestal.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateSerialNumberException.class,
                () -> service.create(request("XYZ-PED-001", 3L)));
    }

    @Test
    void findByIdRejectsUnknownPedestal() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(PedestalNotFoundException.class, () -> service.findById(42L));
    }

    @Test
    void updateAllowsKeepingOwnSerialNumber() {
        Pedestal existing = pedestal(1L, "XYZ-PED-001");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(portClient.getPort(3L)).thenReturn(new PortResponse(3L, "Mahdia", "Mahdia", "Tunisie", 200));
        when(repository.findBySerialNumber("XYZ-PED-001")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var response = service.update(1L, request("XYZ-PED-001", 3L));

        assertEquals("XYZ-PED-001", response.serialNumber());
        verify(repository).save(existing);
    }

    @Test
    void updateRejectsSerialNumberUsedByAnotherPedestal() {
        Pedestal existing = pedestal(1L, "XYZ-PED-001");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(portClient.getPort(3L)).thenReturn(new PortResponse(3L, "Mahdia", "Mahdia", "Tunisie", 200));
        when(repository.findBySerialNumber("XYZ-PED-002")).thenReturn(Optional.of(pedestal(2L, "XYZ-PED-002")));

        assertThrows(DuplicateSerialNumberException.class,
                () -> service.update(1L, request("XYZ-PED-002", 3L)));
        verify(repository, never()).save(any());
    }

    private PedestalRequest request(String serialNumber, Long portId) {
        return new PedestalRequest(serialNumber, "Borne Ponton A1", portId, "A", PedestalStatus.ACTIVE);
    }

    private Pedestal pedestal(Long id, String serialNumber) {
        Pedestal pedestal = new Pedestal();
        pedestal.setId(id);
        pedestal.setSerialNumber(serialNumber);
        pedestal.setName("Borne Ponton A1");
        pedestal.setPortId(3L);
        pedestal.setPontoon("A");
        pedestal.setStatus(PedestalStatus.ACTIVE);
        return pedestal;
    }
}
