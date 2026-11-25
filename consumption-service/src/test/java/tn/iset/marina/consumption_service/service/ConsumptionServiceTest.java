package tn.iset.marina.consumption_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import tn.iset.marina.consumption_service.client.PedestalClient;
import tn.iset.marina.consumption_service.dto.ConsumptionRequest;
import tn.iset.marina.consumption_service.dto.PedestalResponse;
import tn.iset.marina.consumption_service.entity.Consumption;
import tn.iset.marina.consumption_service.entity.ConsumptionType;
import tn.iset.marina.consumption_service.event.ConsumptionRecordedEvent;
import tn.iset.marina.consumption_service.exception.ForbiddenPortAccessException;
import tn.iset.marina.consumption_service.exception.PedestalNotActiveException;
import tn.iset.marina.consumption_service.exception.PedestalNotFoundException;
import tn.iset.marina.consumption_service.repository.ConsumptionRepository;
import tn.iset.marina.consumption_service.security.CurrentUser;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceTest {

    @Mock
    private ConsumptionRepository repository;

    @Mock
    private PedestalClient pedestalClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ConsumptionService service;

    @BeforeEach
    void setUp() {
        service = new ConsumptionService(repository, pedestalClient, eventPublisher);
    }

    @Test
    void recordSavesConsumptionAndPublishesEvent() {
        when(pedestalClient.getPedestal(5L)).thenReturn(pedestal("ACTIVE"));
        when(repository.save(any(Consumption.class))).thenAnswer(invocation -> {
            Consumption saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var response = service.record(request(), CurrentUser.of("u1", "PORT_MANAGER", "3"));

        assertEquals(10L, response.id());
        assertEquals(3L, response.portId());
        assertEquals("kWh", response.unit());
        ArgumentCaptor<ConsumptionRecordedEvent> event = ArgumentCaptor.forClass(ConsumptionRecordedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(10L, event.getValue().consumptionId());
        assertEquals("XYZ-PED-005", event.getValue().pedestalSerialNumber());
        assertEquals("ELECTRICITY", event.getValue().type());
    }

    @Test
    void recordRefusesInactivePedestal() {
        when(pedestalClient.getPedestal(5L)).thenReturn(pedestal("MAINTENANCE"));

        assertThrows(PedestalNotActiveException.class, () -> service.record(request(), CurrentUser.SYSTEM));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recordPropagatesUnknownPedestal() {
        when(pedestalClient.getPedestal(5L)).thenThrow(new PedestalNotFoundException(5L));

        assertThrows(PedestalNotFoundException.class, () -> service.record(request(), CurrentUser.SYSTEM));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recordRefusesManagerOfAnotherPort() {
        when(pedestalClient.getPedestal(5L)).thenReturn(pedestal("ACTIVE"));

        assertThrows(ForbiddenPortAccessException.class,
                () -> service.record(request(), CurrentUser.of("u2", "PORT_MANAGER", "7")));
        verify(repository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void managerListIsRestrictedToOwnPort() {
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(Page.empty());

        service.findAll(PageRequest.of(0, 20), null, null, null, CurrentUser.of("u1", "PORT_MANAGER", "3"));

        assertThrows(ForbiddenPortAccessException.class, () -> service.findAll(PageRequest.of(0, 20),
                null, 7L, null, CurrentUser.of("u1", "PORT_MANAGER", "3")));
    }

    @Test
    void summaryAggregatesByType() {
        when(repository.countByPedestalId(5L)).thenReturn(3L);
        when(repository.sumQuantity(5L, ConsumptionType.ELECTRICITY)).thenReturn(new BigDecimal("42.500"));
        when(repository.sumQuantity(5L, ConsumptionType.WATER)).thenReturn(BigDecimal.ONE);

        var summary = service.summary(5L);

        assertEquals(3L, summary.count());
        assertEquals(new BigDecimal("42.500"), summary.totalElectricityKwh());
    }

    private ConsumptionRequest request() {
        Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
        return new ConsumptionRequest(5L, ConsumptionType.ELECTRICITY, new BigDecimal("12.5"),
                "Sea Breeze", end.minus(3, ChronoUnit.HOURS), end);
    }

    private PedestalResponse pedestal(String status) {
        return new PedestalResponse(5L, "XYZ-PED-005", "Borne B5", 3L, status);
    }
}
