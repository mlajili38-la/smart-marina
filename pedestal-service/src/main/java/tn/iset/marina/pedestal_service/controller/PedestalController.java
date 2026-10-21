package tn.iset.marina.pedestal_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.marina.pedestal_service.dto.PedestalPageResponse;
import tn.iset.marina.pedestal_service.dto.PedestalRequest;
import tn.iset.marina.pedestal_service.dto.PedestalResponse;
import tn.iset.marina.pedestal_service.entity.PedestalStatus;
import tn.iset.marina.pedestal_service.service.PedestalService;

@RestController
@RequestMapping("/api/pedestals")
@RequiredArgsConstructor
public class PedestalController {

    private final PedestalService service;

    @GetMapping
    @Operation(summary = "Lister les bornes avec pagination, tri et filtres")
    public PedestalPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Long portId,
            @RequestParam(required = false) PedestalStatus status
    ) {
        // Le champ et la direction sont deux paramètres distincts : pas d'ambiguïté sur la virgule
        Sort sorting = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return service.findAll(page, size, sorting, portId, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une borne")
    public PedestalResponse getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une borne")
    public PedestalResponse create(@Valid @RequestBody PedestalRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une borne")
    public PedestalResponse update(@PathVariable Long id, @Valid @RequestBody PedestalRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une borne")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}