package tn.iset.marina.pedestal_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.marina.pedestal_service.dto.PedestalPageResponse;
import tn.iset.marina.pedestal_service.dto.PedestalRequest;
import tn.iset.marina.pedestal_service.dto.PedestalResponse;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;
import tn.iset.marina.pedestal_service.security.CurrentUser;
import tn.iset.marina.pedestal_service.service.PedestalService;

@RestController
@RequestMapping("/api/pedestals")
@RequiredArgsConstructor
public class PedestalController {

    private final PedestalService service;

    @GetMapping
    @Operation(summary = "Lister les bornes avec pagination, tri (sort=champ,asc|desc) et filtres")
    public PedestalPageResponse getAll(
            @PageableDefault(size = 20, sort = "id") @Parameter(hidden = true) Pageable pageable,
            @RequestParam(required = false) Long portId,
            @RequestParam(required = false) PedestalStatus status
    ) {
        return service.findAll(pageable, portId, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une borne")
    public PedestalResponse getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une borne")
    public PedestalResponse create(@Valid @RequestBody PedestalRequest request,
                                   @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
                                   @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                   @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String portId) {
        return service.create(request, CurrentUser.of(userId, roles, portId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une borne")
    public PedestalResponse update(@PathVariable Long id, @Valid @RequestBody PedestalRequest request,
                                   @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
                                   @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                   @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String portId) {
        return service.update(id, request, CurrentUser.of(userId, roles, portId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une borne (refusé si elle a des consommations, RG-08)")
    public void delete(@PathVariable Long id,
                       @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
                       @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles,
                       @Parameter(hidden = true) @RequestHeader(value = "X-User-Port-Id", required = false) String portId) {
        service.delete(id, CurrentUser.of(userId, roles, portId));
    }
}
