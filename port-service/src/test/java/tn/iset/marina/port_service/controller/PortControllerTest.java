package tn.iset.marina.port_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.iset.marina.port_service.dto.PortResponse;
import tn.iset.marina.port_service.exception.PedestalServiceUnavailableException;
import tn.iset.marina.port_service.exception.PortInUseException;
import tn.iset.marina.port_service.exception.PortNotFoundException;
import tn.iset.marina.port_service.service.PortService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortController.class)
class PortControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortService service;

    @Test
    void listReturnsPorts() throws Exception {
        when(service.findAll()).thenReturn(List.of(port(1L)));

        mockMvc.perform(get("/api/ports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Marina Mahdia"));
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(service.create(any())).thenReturn(port(1L));

        mockMvc.perform(post("/api/ports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Marina Mahdia\",\"city\":\"Mahdia\",\"country\":\"Tunisie\",\"capacity\":200}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRejectsBlankNameAndNegativeCapacity() throws Exception {
        mockMvc.perform(post("/api/ports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"capacity\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void getReturnsNotFoundWithErrorBody() throws Exception {
        when(service.findById(99L)).thenThrow(new PortNotFoundException(99L));

        mockMvc.perform(get("/api/ports/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Port introuvable : 99"))
                .andExpect(jsonPath("$.path").value("/api/ports/99"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/ports/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUnknownReturnsNotFound() throws Exception {
        doThrow(new PortNotFoundException(5L)).when(service).delete(5L);

        mockMvc.perform(delete("/api/ports/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePortWithPedestalsReturnsConflict() throws Exception {
        doThrow(new PortInUseException(1L, 3)).when(service).delete(1L);

        mockMvc.perform(delete("/api/ports/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Suppression impossible : le port 1 possède encore 3 borne(s)"));
    }

    @Test
    void deleteReturnsServiceUnavailableWhenPedestalServiceIsDown() throws Exception {
        doThrow(new PedestalServiceUnavailableException()).when(service).delete(1L);

        mockMvc.perform(delete("/api/ports/1"))
                .andExpect(status().isServiceUnavailable());
    }

    private PortResponse port(Long id) {
        return new PortResponse(id, "Marina Mahdia", "Mahdia", "Tunisie", 200);
    }
}
