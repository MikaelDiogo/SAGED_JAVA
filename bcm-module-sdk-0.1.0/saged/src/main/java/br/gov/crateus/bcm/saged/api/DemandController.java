package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.AssignDemandRequest;
import br.gov.crateus.bcm.saged.api.dto.ChangeStatusRequest;
import br.gov.crateus.bcm.saged.api.dto.CreateDemandRequest;
import br.gov.crateus.bcm.saged.api.dto.DemandHistoryResponse;
import br.gov.crateus.bcm.saged.api.dto.DemandResponse;
import br.gov.crateus.bcm.saged.api.dto.UpdateNoteRequest;
import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saged/demands")
@Tag(name = "saged-demands")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Create a demand")
    public ResponseEntity<DemandResponse> create(@RequestBody @Valid CreateDemandRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        UUID requesterUserId = request.getRequesterUserId() != null
            ? request.getRequesterUserId()
            : UUID.fromString(jwt.getSubject());
        UUID departmentId = request.getDepartmentId() != null
            ? request.getDepartmentId()
            : resolveOrgUnitId(jwt);
        DemandEntity demand = demandService.create(
            request.getTitle(), request.getDescription(), request.getSpecialtyCode(),
            request.getAssetTag(), requesterUserId, departmentId, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(DemandResponse.from(demand));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List demands — visibility filtered by JWT role")
    public List<DemandResponse> list(@AuthenticationPrincipal Jwt jwt) {
        String role = resolveTopRole();
        UUID orgUnitId = resolveOrgUnitId(jwt);
        List<String> specialtyCodes = jwt.getClaimAsStringList("specialty_codes");
        return demandService.list(role, orgUnitId, specialtyCodes != null ? specialtyCodes : List.of())
            .stream().map(DemandResponse::from).toList();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER')")
    @Operation(summary = "Change demand status")
    public DemandResponse changeStatus(@PathVariable UUID id,
                                        @RequestBody @Valid ChangeStatusRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        DemandEntity demand = demandService.changeStatus(
            id, request.getStatus(), request.getJustification(), jwt.getSubject());
        return DemandResponse.from(demand);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get demand by ID")
    public DemandResponse getById(@PathVariable UUID id) {
        return DemandResponse.from(demandService.findById(id));
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER')")
    @Operation(summary = "Assign a technician to a demand")
    public DemandResponse assign(@PathVariable UUID id,
                                  @RequestBody @Valid AssignDemandRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return DemandResponse.from(demandService.assign(id, request.getAssigneeUserId(), jwt.getSubject()));
    }

    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Update technical note on a demand")
    public DemandResponse updateNote(@PathVariable UUID id,
                                      @RequestBody @Valid UpdateNoteRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return DemandResponse.from(demandService.updateNote(id, request.getNote(), jwt.getSubject()));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get demand history")
    public List<DemandHistoryResponse> getHistory(@PathVariable UUID id) {
        return demandService.getHistory(id).stream().map(DemandHistoryResponse::from).toList();
    }

    private String resolveTopRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        for (var a : auth.getAuthorities()) {
            if (a.getAuthority().equals("SAGED_ADMIN_GERAL")) return "SAGED_ADMIN_GERAL";
        }
        for (var a : auth.getAuthorities()) {
            if (a.getAuthority().equals("SAGED_ADMIN_SETOR")) return "SAGED_ADMIN_SETOR";
        }
        return "SAGED_TECNICO";
    }

    private UUID resolveOrgUnitId(Jwt jwt) {
        String raw = jwt.getClaimAsString("org_unit_id");
        return raw != null ? UUID.fromString(raw) : null;
    }
}
