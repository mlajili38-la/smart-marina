package tn.iset.marina.consumption_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.iset.marina.consumption_service.dto.ConsumptionResponse;
import tn.iset.marina.consumption_service.dto.ConsumptionSummaryResponse;
import tn.iset.marina.consumption_service.entity.ConsumptionType;
import tn.iset.marina.consumption_service.exception.PedestalNotActiveException;
import tn.iset.marina.consumption_service.exception.PedestalNotFoundException;
import tn.iset.marina.consumption_service.service.ConsumptionService;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsumptionController.class)
class ConsumptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsumptionService service;

    @Test
    void recordReturnsCreated() throws Exception {
        Instant now = Instant.parse("2026-11-10T12:00:00Z");
        when(service.record(any(), any())).thenReturn(new ConsumptionResponse(1L, 5L, 3L,
                ConsumptionType.WATER, new BigDecimal("0.750"), "m3", null, now.minusSeconds(3600), now, now));

        mockMvc.perform(post("/api/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unit").value("m3"));
    }

    @Test
    void recordRejectsNegativeQuantityAndInvertedPeriod() throws Exception {
        String body = "{\"pedestalId\":5,\"type\":\"WATER\",\"quantity\":-1,"
                + "\"startedAt\":\"2026-11-10T12:00:00Z\",\"endedAt\":\"2026-11-10T10:00:00Z\"}";

        mockMvc.perform(post("/api/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void recordReturnsBadRequestForUnknownPedestal() throws Exception {
        when(service.record(any(), any())).thenThrow(new PedestalNotFoundException(5L));

        mockMvc.perform(post("/api/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("pedestalId"));
    }

    @Test
    void recordReturnsConflictForInactivePedestal() throws Exception {
        when(service.record(any(), any())).thenThrow(new PedestalNotActiveException(5L, "MAINTENANCE"));

        mockMvc.perform(post("/api/consumptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void summaryReturnsCount() throws Exception {
        when(service.summary(5L)).thenReturn(new ConsumptionSummaryResponse(5L, 2, BigDecimal.TEN, BigDecimal.ZERO));

        mockMvc.perform(get("/api/consumptions/summary?pedestalId=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void summaryRequiresPedestalId() throws Exception {
        mockMvc.perform(get("/api/consumptions/summary"))
                .andExpect(status().isBadRequest());
    }

    private String validBody() {
        return "{\"pedestalId\":5,\"type\":\"WATER\",\"quantity\":0.75,\"boatName\":\"Sea Breeze\","
                + "\"startedAt\":\"2026-11-10T10:00:00Z\",\"endedAt\":\"2026-11-10T12:00:00Z\"}";
    }
}
