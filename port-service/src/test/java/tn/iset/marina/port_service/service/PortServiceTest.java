package tn.iset.marina.port_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iset.marina.port_service.dto.PortRequest;
import tn.iset.marina.port_service.entity.Port;
import tn.iset.marina.port_service.exception.PortNotFoundException;
import tn.iset.marina.port_service.repository.PortRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortServiceTest {

    @Mock
    private PortRepository repository;

    private PortService service;

    @BeforeEach
    void setUp() {
        service = new PortService(repository);
    }

    @Test
    void createCopiesAllFields() {
        when(repository.save(any(Port.class))).thenAnswer(invocation -> {
            Port port = invocation.getArgument(0);
            port.setId(1L);
            return port;
        });

        var response = service.create(new PortRequest("Marina Mahdia", "Mahdia", "Tunisie", 200));

        assertEquals(1L, response.id());
        assertEquals("Mahdia", response.city());
        assertEquals(200, response.capacity());
    }

    @Test
    void updateUnknownPortThrows() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PortNotFoundException.class,
                () -> service.update(9L, new PortRequest("X", null, null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteUnknownPortThrows() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PortNotFoundException.class, () -> service.delete(9L));
        verify(repository, never()).delete(any());
    }
}
