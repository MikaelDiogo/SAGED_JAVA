package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.CreateSpecialtyRequest;
import br.gov.crateus.bcm.saged.api.dto.SpecialtyResponse;
import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/saged/specialties")
@Tag(name = "saged-specialties")
public class SpecialtyController {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyController(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List all specialties")
    public List<SpecialtyResponse> list() {
        return specialtyRepository.findAll().stream().map(SpecialtyResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Create a specialty (ADMIN_GERAL only)")
    public ResponseEntity<SpecialtyResponse> create(@RequestBody @Valid CreateSpecialtyRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        String code = request.getCode().toUpperCase();
        if (specialtyRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Specialty code already exists: " + code);
        }
        SpecialtyEntity e = new SpecialtyEntity();
        e.setCode(code);
        e.setName(request.getName());
        String actor = jwt.getSubject();
        e.setCreatedBy(actor);
        e.setUpdatedBy(actor);
        e = specialtyRepository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpecialtyResponse.from(e));
    }
}
