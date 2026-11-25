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

import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tn.iset.marina.pedestal_service.dto.PedestalPageResponse;
import tn.iset.marina.pedestal_service.exception.ForbiddenPortAccessException;
import tn.iset.marina.pedestal_service.exception.PedestalInUseException;
import tn.iset.marina.pedestal_service.security.CurrentUser;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        when(service.create(any(), any())).thenReturn(response(1L));

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
        when(service.update(anyLong(), any(), any())).thenThrow(new DuplicateSerialNumberException("XYZ-PED-002"));

        mockMvc.perform(put("/api/pedestals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void listAcceptsSortParameterFromDocumentation() throws Exception {
        when(service.findAll(any(), any(), any())).thenReturn(new PedestalPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/pedestals?page=0&size=20&sort=name,desc&portId=3&status=ACTIVE"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(pageable.capture(), eq(3L), eq(PedestalStatus.ACTIVE));
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("name").getDirection());
    }

    @Test
    void createPassesGatewayIdentityToService() throws Exception {
        when(service.create(any(), any())).thenReturn(response(1L));

        mockMvc.perform(post("/api/pedestals")
                        .header("X-User-Id", "u1")
                        .header("X-User-Roles", "PORT_MANAGER")
                        .header("X-User-Port-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated());

        ArgumentCaptor<CurrentUser> user = ArgumentCaptor.forClass(CurrentUser.class);
        verify(service).create(any(), user.capture());
        assertEquals(3L, user.getValue().portId());
        assertEquals(Set.of("PORT_MANAGER"), user.getValue().roles());
    }

    @Test
    void createReturnsForbiddenForAnotherPort() throws Exception {
        when(service.create(any(), any())).thenThrow(new ForbiddenPortAccessException(3L));

        mockMvc.perform(post("/api/pedestals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReturnsConflictWhenPedestalHasConsumptions() throws Exception {
        doThrow(new PedestalInUseException(1L, 2)).when(service).delete(anyLong(), any());

        mockMvc.perform(delete("/api/pedestals/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Suppression impossible : la borne 1 possède 2 consommation(s)"));
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
