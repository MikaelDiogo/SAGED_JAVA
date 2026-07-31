package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.AssignDemandRequest;
import br.gov.crateus.bcm.saged.api.dto.ChangeStatusRequest;
import br.gov.crateus.bcm.saged.api.dto.CreateDemandRequest;
import br.gov.crateus.bcm.saged.api.dto.DemandHistoryResponse;
import br.gov.crateus.bcm.saged.api.dto.DemandResponse;
import br.gov.crateus.bcm.saged.api.dto.UpdateEquipmentRequest;
import br.gov.crateus.bcm.saged.api.dto.UpdateNoteRequest;
import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
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
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List demands with optional filters — visibility enforced by role")
    public Page<DemandResponse> list(
            @RequestParam(required = false) DemandStatus status,
            @RequestParam(required = false) UUID specialtyId,
            @RequestParam(required = false) UUID departmentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        String role = resolveTopRole();
        UUID orgUnitId = resolveOrgUnitId(jwt);
        List<String> specialtyCodes = jwt.getClaimAsStringList("specialty_codes");
        return demandService.list(role, orgUnitId, specialtyCodes != null ? specialtyCodes : List.of(),
                status, specialtyId, departmentId, pageable)
            .map(DemandResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get demand by ID")
    public DemandResponse getById(@PathVariable UUID id) {
        return DemandResponse.from(demandService.findById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Change demand status")
    public DemandResponse changeStatus(@PathVariable UUID id,
                                        @RequestBody @Valid ChangeStatusRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return DemandResponse.from(demandService.changeStatus(
            id, request.getStatus(), request.getJustification(), jwt.getSubject()));
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Assign a technician to a demand")
    public DemandResponse assign(@PathVariable UUID id,
                                  @RequestBody @Valid AssignDemandRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return DemandResponse.from(demandService.assign(id, request.getAssigneeUserId(), jwt.getSubject()));
    }

    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Update technical note on a demand")
    public DemandResponse updateNote(@PathVariable UUID id,
                                      @RequestBody @Valid UpdateNoteRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return DemandResponse.from(demandService.updateNote(id, request.getNote(), jwt.getSubject()));
    }

    @PatchMapping("/{id}/equipment")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Register equipment details on a demand")
    public DemandResponse updateEquipment(@PathVariable UUID id,
                                           @RequestBody UpdateEquipmentRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        return DemandResponse.from(demandService.updateEquipment(
            id, request.getIsRented(), request.getAssetTag(),
            request.getEquipmentName(), request.getEquipmentModel(), jwt.getSubject()));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get demand history")
    public List<DemandHistoryResponse> getHistory(@PathVariable UUID id) {
        return demandService.getHistory(id).stream().map(DemandHistoryResponse::from).toList();
    }

    private String resolveTopRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        for (var a : auth.getAuthorities()) {
            if (a.getAuthority().equals("ROLE_SAGED_ADMIN_GERAL")) return "SAGED_ADMIN_GERAL";
        }
        for (var a : auth.getAuthorities()) {
            if (a.getAuthority().equals("ROLE_SAGED_ADMIN_SETOR")) return "SAGED_ADMIN_SETOR";
        }
        for (var a : auth.getAuthorities()) {
            if (a.getAuthority().equals("ROLE_SAGED_TECNICO_LIDER")) return "SAGED_TECNICO_LIDER";
        }
        return "SAGED_TECNICO";
    }

    private UUID resolveOrgUnitId(Jwt jwt) {
        String raw = jwt.getClaimAsString("org_unit_id");
        return raw != null ? UUID.fromString(raw) : null;
    }
}
