package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.OrgUnitResponse;
import br.gov.crateus.bcm.saged.infrastructure.repository.OrgUnitRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saged/org-units")
@Tag(name = "saged-org-units")
public class OrgUnitController {

    private final OrgUnitRepository orgUnitRepository;

    public OrgUnitController(OrgUnitRepository orgUnitRepository) {
        this.orgUnitRepository = orgUnitRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List all org units known to SAGED")
    public List<OrgUnitResponse> list() {
        return orgUnitRepository.findAll().stream().map(OrgUnitResponse::from).toList();
    }
}
