package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.CreateTelegramRequesterRequest;
import br.gov.crateus.bcm.saged.api.dto.TelegramRequesterResponse;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/saged/telegram/requesters")
@Tag(name = "saged-telegram")
public class TelegramRequesterController {

    private final TelegramRequesterRepository repository;

    public TelegramRequesterController(TelegramRequesterRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR')")
    @Operation(summary = "List Telegram requesters (all for ADMIN_GERAL, own unit for ADMIN_SETOR)")
    public List<TelegramRequesterResponse> list(Authentication auth, @AuthenticationPrincipal Jwt jwt) {
        if (isAdminGeral(auth)) {
            return repository.findAll().stream().map(TelegramRequesterResponse::from).toList();
        }
        UUID deptId = extractOrgUnitId(jwt);
        return repository.findByDepartmentId(deptId).stream().map(TelegramRequesterResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR')")
    @Operation(summary = "Manually register an active Telegram requester")
    public ResponseEntity<TelegramRequesterResponse> create(
            @RequestBody @Valid CreateTelegramRequesterRequest request,
            Authentication auth,
            @AuthenticationPrincipal Jwt jwt) {
        if (repository.existsByTelegramChatId(request.getTelegramChatId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Telegram chat ID already registered: " + request.getTelegramChatId());
        }
        UUID deptId = isAdminGeral(auth) ? request.getDepartmentId() : extractOrgUnitId(jwt);
        TelegramRequesterEntity e = new TelegramRequesterEntity();
        e.setTelegramChatId(request.getTelegramChatId());
        e.setPhoneNumber(request.getPhoneNumber());
        e.setDisplayName(request.getDisplayName());
        e.setDepartmentId(deptId);
        e.setStatus(TelegramRequesterStatus.ACTIVE);
        e.setCreatedBy(jwt.getSubject());
        e.setUpdatedBy(jwt.getSubject());
        e = repository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(TelegramRequesterResponse.from(e));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR')")
    @Operation(summary = "Approve a pending Telegram requester")
    public TelegramRequesterResponse approve(
            @PathVariable UUID id,
            @RequestParam(name = "departmentId", required = false) String departmentIdStr,
            Authentication auth,
            @AuthenticationPrincipal Jwt jwt) {
        TelegramRequesterEntity e = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + id));

        if (e.getStatus() != TelegramRequesterStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requesters can be approved");
        }

        if (isAdminGeral(auth)) {
            UUID deptId = (departmentIdStr != null && !departmentIdStr.isBlank())
                ? UUID.fromString(departmentIdStr)
                : e.getDepartmentId();
            if (deptId == null) throw new IllegalArgumentException("departmentId é obrigatório para aprovar");
            e.setDepartmentId(deptId);
        } else {
            e.setDepartmentId(extractOrgUnitId(jwt));
        }

        e.setStatus(TelegramRequesterStatus.ACTIVE);
        e.setUpdatedBy(jwt.getSubject());
        return TelegramRequesterResponse.from(repository.save(e));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR')")
    @Operation(summary = "Reject a pending Telegram requester")
    public TelegramRequesterResponse reject(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        TelegramRequesterEntity e = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + id));
        if (e.getStatus() != TelegramRequesterStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requesters can be rejected");
        }
        e.setStatus(TelegramRequesterStatus.INACTIVE);
        e.setUpdatedBy(jwt.getSubject());
        return TelegramRequesterResponse.from(repository.save(e));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR')")
    @Operation(summary = "Deactivate an active Telegram requester")
    public TelegramRequesterResponse deactivate(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        TelegramRequesterEntity e = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + id));
        e.setStatus(TelegramRequesterStatus.INACTIVE);
        e.setUpdatedBy(jwt.getSubject());
        return TelegramRequesterResponse.from(repository.save(e));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR')")
    @Operation(summary = "Reactivate an inactive Telegram requester")
    public TelegramRequesterResponse activate(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        TelegramRequesterEntity e = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + id));
        if (e.getDepartmentId() == null) {
            throw new IllegalArgumentException("Cannot activate a requester without a departmentId");
        }
        e.setStatus(TelegramRequesterStatus.ACTIVE);
        e.setUpdatedBy(jwt.getSubject());
        return TelegramRequesterResponse.from(repository.save(e));
    }

    private static boolean isAdminGeral(Authentication auth) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SAGED_ADMIN_GERAL"));
    }

    private static UUID extractOrgUnitId(Jwt jwt) {
        String raw = jwt.getClaimAsString("org_unit_id");
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "org_unit_id claim missing in token");
        }
        return UUID.fromString(raw);
    }
}
