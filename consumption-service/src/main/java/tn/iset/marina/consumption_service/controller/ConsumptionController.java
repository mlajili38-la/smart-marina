package tn.iset.marina.consumption_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.marina.consumption_service.dto.ConsumptionPageResponse;
import tn.iset.marina.consumption_service.dto.ConsumptionRequest;
import tn.iset.marina.consumption_service.dto.ConsumptionResponse;
import tn.iset.marina.consumption_service.dto.ConsumptionSummaryResponse;
import tn.iset.marina.consumption_service.entity.ConsumptionType;
import tn.iset.marina.consumption_service.security.CurrentUser;
import tn.iset.marina.consumption_service.service.ConsumptionService;

@RestController
@RequestMapping("/api/consumptions")
@RequiredArgsConstructor
public class ConsumptionController {

    private final ConsumptionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enregistrer une consommation (publie l'événement Kafka consumption-recorded)")
    public ConsumptionResponse record(@Valid @RequestBody ConsumptionRequest request,
                                      @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
                                      @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                      @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String portId) {
        return service.record(request, CurrentUser.of(userId, roles, portId));
    }

    @GetMapping
    @Operation(summary = "Lister les consommations (filtres pedestalId, portId, type ; tri sort=champ,asc|desc)")
    public ConsumptionPageResponse getAll(
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) Long pedestalId,
            @RequestParam(required = false) Long portId,
            @RequestParam(required = false) ConsumptionType type,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String userPortId) {
        return service.findAll(pageable, pedestalId, portId, type, CurrentUser.of(userId, roles, userPortId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Bilan d'une borne : nombre de consommations et totaux (utilisé par la règle RG-08)")
    public ConsumptionSummaryResponse summary(@RequestParam Long pedestalId) {
        return service.summary(pedestalId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une consommation")
    public ConsumptionResponse getOne(@PathVariable Long id,
                                      @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
                                      @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                      @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String portId) {
        return service.findById(id, CurrentUser.of(userId, roles, portId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une consommation saisie par erreur (ADMIN)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
