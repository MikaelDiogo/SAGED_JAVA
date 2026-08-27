package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.AssignDemandRequest;
import br.gov.crateus.bcm.saged.api.dto.ChangeStatusRequest;
import br.gov.crateus.bcm.saged.api.dto.CreateDemandRequest;
import br.gov.crateus.bcm.saged.api.dto.DemandHistoryResponse;
import br.gov.crateus.bcm.saged.api.dto.DemandResponse;
import br.gov.crateus.bcm.saged.api.dto.DemandViewResponse;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandViewEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.DemandViewRepository;
import br.gov.crateus.bcm.saged.api.dto.UpdateEquipmentRequest;
import br.gov.crateus.bcm.saged.api.dto.UpdateNoteRequest;
import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.application.TelegramBotService;
import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/saged/demands")
@Tag(name = "saged-demands")
public class DemandController {

    private final DemandService demandService;
    private final TelegramBotService telegramBotService;
    private final DemandViewRepository viewRepository;

    public DemandController(DemandService demandService,
                             TelegramBotService telegramBotService,
                             DemandViewRepository viewRepository) {
        this.demandService = demandService;
        this.telegramBotService = telegramBotService;
        this.viewRepository = viewRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Create a demand")
    public ResponseEntity<DemandResponse> create(@RequestBody @Valid CreateDemandRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        String role = resolveTopRole();
        boolean isAdminGeral = "SAGED_ADMIN_GERAL".equals(role);

        UUID requesterUserId = isAdminGeral && request.getRequesterUserId() != null
            ? request.getRequesterUserId()
            : UUID.fromString(jwt.getSubject());

        UUID departmentId = isAdminGeral && request.getDepartmentId() != null
            ? request.getDepartmentId()
            : resolveOrgUnitId(jwt);

        if (departmentId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "org_unit_id claim is required for this role");
        }

        DemandEntity demand = demandService.create(
            request.getTitle(), request.getDescription(), request.getSpecialtyCode(),
            request.getAssetTag(), requesterUserId, departmentId, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(DemandResponse.from(demand));
    }

    private static final int MAX_PAGE_SIZE = 200;

    @GetMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List demands with optional filters — visibility enforced by role")
    public Page<DemandResponse> list(
            @RequestParam(required = false) DemandStatus status,
            @RequestParam(required = false) UUID specialtyId,
            @RequestParam(required = false) UUID departmentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        Pageable bounded = pageable.getPageSize() > MAX_PAGE_SIZE
            ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort())
            : pageable;
        String role = resolveTopRole();
        UUID orgUnitId = resolveOrgUnitId(jwt);
        if (("SAGED_ADMIN_SETOR".equals(role) || "SAGED_TECNICO_LIDER".equals(role)) && orgUnitId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "org_unit_id claim is required for this role");
        }
        List<String> specialtyCodes = resolveSpecialtyCodes(jwt);
        return demandService.list(role, orgUnitId, specialtyCodes,
                status, specialtyId, departmentId, bounded)
            .map(DemandResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get demand by ID")
    public DemandResponse getById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        DemandEntity demand = demandService.findById(id);
        verifyDemandAccess(demand, jwt);
        return DemandResponse.from(demand);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Change demand status")
    public DemandResponse changeStatus(@PathVariable UUID id,
                                        @RequestBody @Valid ChangeStatusRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        verifyDemandAccess(demandService.findById(id), jwt);
        DemandEntity demand = demandService.changeStatus(
            id, request.getStatus(), request.getJustification(), resolveActorName(jwt));
        String actorName = resolveActorName(jwt);
        if (request.getStatus() == br.gov.crateus.bcm.saged.domain.DemandStatus.DONE) {
            telegramBotService.notifyDemandConcluded(demand, actorName, request.getJustification());
        } else if (request.getStatus() == br.gov.crateus.bcm.saged.domain.DemandStatus.INTERRUPTED) {
            telegramBotService.notifyDemandInterrupted(demand, actorName, request.getJustification());
        }
        return DemandResponse.from(demand);
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Assign a technician to a demand")
    public DemandResponse assign(@PathVariable UUID id,
                                  @RequestBody @Valid AssignDemandRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        verifyDemandAccess(demandService.findById(id), jwt);
        String assigneeName = jwt.getClaimAsString("preferred_username");
        DemandEntity demand = demandService.assign(id, request.getAssigneeUserId(), resolveActorName(jwt));
        telegramBotService.notifyDemandAssigned(demand, assigneeName != null ? assigneeName : "Tecnico");
        return DemandResponse.from(demand);
    }

    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Update technical note on a demand")
    public DemandResponse updateNote(@PathVariable UUID id,
                                      @RequestBody @Valid UpdateNoteRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        verifyDemandAccess(demandService.findById(id), jwt);
        return DemandResponse.from(demandService.updateNote(id, request.getNote(), resolveActorName(jwt)));
    }

    @PatchMapping("/{id}/equipment")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Register equipment details on a demand")
    public DemandResponse updateEquipment(@PathVariable UUID id,
                                           @RequestBody UpdateEquipmentRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        verifyDemandAccess(demandService.findById(id), jwt);
        return DemandResponse.from(demandService.updateEquipment(
            id, request.getIsRented(), request.getAssetTag(),
            request.getEquipmentName(), request.getEquipmentModel(), resolveActorName(jwt)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get demand history")
    public List<DemandHistoryResponse> getHistory(@PathVariable UUID id) {
        return demandService.getHistory(id).stream().map(DemandHistoryResponse::from).toList();
    }

    @PostMapping("/{id}/view")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Mark demand as viewed by current user")
    public org.springframework.http.ResponseEntity<Void> markViewed(@PathVariable UUID id,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = UUID.fromString(jwt.getSubject());
        if (!viewRepository.existsByDemandIdAndViewerUserId(id, viewerId)) {
            DemandEntity demand = demandService.findById(id);
            DemandViewEntity view = new DemandViewEntity();
            view.setDemand(demand);
            view.setViewerUserId(viewerId);
            view.setViewerName(resolveActorName(jwt));
            view.setViewedAt(OffsetDateTime.now(ZoneOffset.UTC));
            view.setCreatedBy(jwt.getSubject());
            view.setUpdatedBy(jwt.getSubject());
            viewRepository.save(view);
        }
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/viewers")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List users who viewed this demand")
    public List<DemandViewResponse> getViewers(@PathVariable UUID id) {
        return viewRepository.findByDemandIdOrderByViewedAtAsc(id).stream()
            .map(DemandViewResponse::from)
            .toList();
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

    private void verifyDemandAccess(DemandEntity demand, Jwt jwt) {
        String role = resolveTopRole();
        switch (role) {
            case "SAGED_ADMIN_GERAL" -> {} // sees all
            case "SAGED_ADMIN_SETOR", "SAGED_TECNICO_LIDER" -> {
                UUID orgUnitId = resolveOrgUnitId(jwt);
                if (orgUnitId == null) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "org_unit_id claim is required for this role");
                }
                if (!orgUnitId.equals(demand.getDepartmentId())) {
                    throw new IllegalArgumentException("Demand not found: " + demand.getId());
                }
            }
            case "SAGED_TECNICO" -> {
                List<String> codes = resolveSpecialtyCodes(jwt);
                if (codes.isEmpty() || demand.getSpecialty() == null || !codes.contains(demand.getSpecialty().getCode())) {
                    throw new IllegalArgumentException("Demand not found: " + demand.getId());
                }
            }
            default -> throw new IllegalArgumentException("Demand not found: " + demand.getId());
        }
    }

    private static String resolveActorName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) return name;
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }

    private UUID resolveOrgUnitId(Jwt jwt) {
        String raw = jwt.getClaimAsString("org_unit_id");
        return raw != null ? UUID.fromString(raw) : null;
    }

    private List<String> resolveSpecialtyCodes(Jwt jwt) {
        Object raw = jwt.getClaim("specialty_codes");
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        String str = raw.toString().trim();
        if (str.isBlank()) return List.of();
        return Arrays.stream(str.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
