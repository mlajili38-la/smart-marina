package tn.iset.marina.pedestal_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tn.iset.marina.pedestal_service.client.ConsumptionClient;
import tn.iset.marina.pedestal_service.client.PortClient;
import tn.iset.marina.pedestal_service.dto.PedestalRequest;
import tn.iset.marina.pedestal_service.dto.PortResponse;
import tn.iset.marina.pedestal_service.entity.Pedestal;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;
import tn.iset.marina.pedestal_service.exception.ConsumptionServiceUnavailableException;
import tn.iset.marina.pedestal_service.exception.DuplicateSerialNumberException;
import tn.iset.marina.pedestal_service.exception.ForbiddenPortAccessException;
import tn.iset.marina.pedestal_service.exception.PedestalInUseException;
import tn.iset.marina.pedestal_service.exception.PedestalNotFoundException;
import tn.iset.marina.pedestal_service.exception.PortNotFoundException;
import tn.iset.marina.pedestal_service.exception.PortServiceUnavailableException;
import tn.iset.marina.pedestal_service.repository.PedestalRepository;
import tn.iset.marina.pedestal_service.security.CurrentUser;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedestalServiceTest {

    private static final CurrentUser ANONYMOUS = CurrentUser.SYSTEM;
    private static final CurrentUser MANAGER_PORT_3 = CurrentUser.of("u1", "PORT_MANAGER", "3");
    private static final CurrentUser MANAGER_PORT_7 = CurrentUser.of("u2", "PORT_MANAGER", "7");
    private static final CurrentUser ADMIN = CurrentUser.of("admin", "ADMIN", null);

    @Mock
    private PedestalRepository repository;

    @Mock
    private PortClient portClient;

    @Mock
    private ConsumptionClient consumptionClient;

    private PedestalService service;

    @BeforeEach
    void setUp() {
        service = new PedestalService(repository, portClient, consumptionClient);
    }

    @Test
    void createSavesPedestalAfterCheckingPort() {
        when(portClient.getPort(3L)).thenReturn(port(3L));
        when(repository.saveAndFlush(any(Pedestal.class))).thenAnswer(invocation -> {
            Pedestal saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(saved.getCreatedAt());
            return saved;
        });

        var response = service.create(request("XYZ-PED-001", 3L), ANONYMOUS);

        assertEquals(1L, response.id());
        assertEquals("XYZ-PED-001", response.serialNumber());
        verify(portClient).getPort(3L);
    }

    @Test
    void createRejectsDuplicateSerialNumber() {
        when(portClient.getPort(3L)).thenReturn(port(3L));
        when(repository.existsBySerialNumber("XYZ-PED-001")).thenReturn(true);

        assertThrows(DuplicateSerialNumberException.class,
                () -> service.create(request("XYZ-PED-001", 3L), ANONYMOUS));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnknownPort() {
        when(portClient.getPort(999L)).thenThrow(new PortNotFoundException(999L));

        assertThrows(PortNotFoundException.class, () -> service.create(request("XYZ-PED-001", 999L), ANONYMOUS));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createReportsUnavailablePortService() {
        when(portClient.getPort(3L)).thenThrow(new PortServiceUnavailableException());

        assertThrows(PortServiceUnavailableException.class,
                () -> service.create(request("XYZ-PED-001", 3L), ANONYMOUS));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createMapsConcurrentUniqueConstraintViolationToConflict() {
        when(portClient.getPort(3L)).thenReturn(port(3L));
        when(repository.saveAndFlush(any(Pedestal.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateSerialNumberException.class,
                () -> service.create(request("XYZ-PED-001", 3L), ANONYMOUS));
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
        when(portClient.getPort(3L)).thenReturn(port(3L));
        when(repository.findBySerialNumber("XYZ-PED-001")).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        var response = service.update(1L, request("XYZ-PED-001", 3L), ANONYMOUS);

        assertEquals("XYZ-PED-001", response.serialNumber());
        verify(repository).saveAndFlush(existing);
    }

    @Test
    void updateRejectsSerialNumberUsedByAnotherPedestal() {
        Pedestal existing = pedestal(1L, "XYZ-PED-001");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(portClient.getPort(3L)).thenReturn(port(3L));
        when(repository.findBySerialNumber("XYZ-PED-002")).thenReturn(Optional.of(pedestal(2L, "XYZ-PED-002")));

        assertThrows(DuplicateSerialNumberException.class,
                () -> service.update(1L, request("XYZ-PED-002", 3L), ANONYMOUS));
        verify(repository, never()).saveAndFlush(any());
    }

    // ----- RG-09 : sécurité par port -----

    @Test
    void portManagerCanCreatePedestalOnOwnPort() {
        when(portClient.getPort(3L)).thenReturn(port(3L));
        when(repository.saveAndFlush(any(Pedestal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request("XYZ-PED-001", 3L), MANAGER_PORT_3);

        verify(repository).saveAndFlush(any(Pedestal.class));
    }

    @Test
    void portManagerCannotCreatePedestalOnAnotherPort() {
        assertThrows(ForbiddenPortAccessException.class,
                () -> service.create(request("XYZ-PED-001", 3L), MANAGER_PORT_7));
        verifyNoInteractions(portClient);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void portManagerCannotMovePedestalToAnotherPort() {
        when(repository.findById(1L)).thenReturn(Optional.of(pedestal(1L, "XYZ-PED-001")));

        assertThrows(ForbiddenPortAccessException.class,
                () -> service.update(1L, request("XYZ-PED-001", 7L), MANAGER_PORT_3));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void boaterCannotModifyPedestal() {
        CurrentUser boater = CurrentUser.of("b1", "BOATER", null);

        assertThrows(ForbiddenPortAccessException.class,
                () -> service.create(request("XYZ-PED-001", 3L), boater));
    }

    // ----- RG-08 : suppression -----

    @Test
    void deleteRemovesPedestalWithoutConsumption() {
        Pedestal existing = pedestal(1L, "XYZ-PED-001");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(consumptionClient.countByPedestal(1L)).thenReturn(0L);

        service.delete(1L, ADMIN);

        verify(repository).delete(existing);
    }

    @Test
    void deleteRefusesPedestalWithConsumptions() {
        when(repository.findById(1L)).thenReturn(Optional.of(pedestal(1L, "XYZ-PED-001")));
        when(consumptionClient.countByPedestal(1L)).thenReturn(4L);

        assertThrows(PedestalInUseException.class, () -> service.delete(1L, ADMIN));
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteReportsUnavailableConsumptionService() {
        when(repository.findById(1L)).thenReturn(Optional.of(pedestal(1L, "XYZ-PED-001")));
        when(consumptionClient.countByPedestal(1L)).thenThrow(new ConsumptionServiceUnavailableException());

        assertThrows(ConsumptionServiceUnavailableException.class, () -> service.delete(1L, ADMIN));
        verify(repository, never()).delete(any());
    }

    private PortResponse port(Long id) {
        return new PortResponse(id, "Mahdia", "Mahdia", "Tunisie", 200);
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
