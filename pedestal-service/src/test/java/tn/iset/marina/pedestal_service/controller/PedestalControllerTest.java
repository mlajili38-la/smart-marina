package tn.iset.marina.pedestal_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tn.iset.marina.pedestal_service.dto.PedestalResponse;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;
import tn.iset.marina.pedestal_service.exception.DuplicateSerialNumberException;
import tn.iset.marina.pedestal_service.exception.PedestalNotFoundException;
import tn.iset.marina.pedestal_service.service.PedestalService;

import java.time.Instant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedestalController.class)
class PedestalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedestalService service;

    @Test
    void createReturnsCreated() throws Exception {
        when(service.create(any())).thenReturn(response(1L));

        mockMvc.perform(post("/api/pedestals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serialNumber").value("XYZ-PED-001"));
    }

    @Test
    void createReturnsFieldErrorsWhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/pedestals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("Borne Ponton A1", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void createReturnsFieldErrorsWhenStatusIsMissing() throws Exception {
        mockMvc.perform(post("/api/pedestals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serialNumber\":\"XYZ-PED-001\",\"name\":\"Borne\",\"portId\":3,\"pontoon\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    void createReturnsBadRequestForUnknownStatus() throws Exception {
        mockMvc.perform(post("/api/pedestals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("ACTIVE", "BROKEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    void getReturnsNotFound() throws Exception {
        when(service.findById(99L)).thenThrow(new PedestalNotFoundException(99L));

        mockMvc.perform(get("/api/pedestals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Borne introuvable : 99"));
    }

    @Test
    void updateReturnsConflictForDuplicateSerialNumber() throws Exception {
        when(service.update(anyLong(), any())).thenThrow(new DuplicateSerialNumberException("XYZ-PED-002"));

        mockMvc.perform(put("/api/pedestals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private String validBody() {
        return "{\"serialNumber\":\"XYZ-PED-001\",\"name\":\"Borne Ponton A1\","
                + "\"portId\":3,\"pontoon\":\"A\",\"status\":\"ACTIVE\"}";
    }

    private PedestalResponse response(Long id) {
        Instant now = Instant.now();
        return new PedestalResponse(id, "XYZ-PED-001", "Borne Ponton A1", 3L, "A",
                PedestalStatus.ACTIVE, now, now);
    }
}
