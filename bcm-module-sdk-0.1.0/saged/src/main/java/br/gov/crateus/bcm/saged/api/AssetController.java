package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.AssetResponse;
import br.gov.crateus.bcm.saged.api.dto.CreateAssetRequest;
import br.gov.crateus.bcm.saged.infrastructure.entity.AssetEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.AssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/saged/assets")
@Tag(name = "saged-assets")
public class AssetController {

    private final AssetRepository assetRepository;

    public AssetController(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List assets")
    public Page<AssetResponse> list(
            @PageableDefault(size = 20, sort = "assetTag", direction = Sort.Direction.ASC) Pageable pageable) {
        return assetRepository.findAll(pageable).map(AssetResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get asset by ID")
    public AssetResponse getById(@PathVariable UUID id) {
        return AssetResponse.from(assetRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + id)));
    }

    @GetMapping("/by-tag/{tag}")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Get asset by asset tag (patrimony number)")
    public AssetResponse getByTag(@PathVariable String tag) {
        return AssetResponse.from(assetRepository.findByAssetTag(tag)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found for tag: " + tag)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR')")
    @Operation(summary = "Create an asset")
    public ResponseEntity<AssetResponse> create(@RequestBody @Valid CreateAssetRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        if (assetRepository.existsByAssetTag(request.getAssetTag())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Asset tag already exists: " + request.getAssetTag());
        }
        AssetEntity e = new AssetEntity();
        e.setAssetTag(request.getAssetTag());
        e.setDescription(request.getDescription());
        e.setCreatedBy(jwt.getSubject());
        e.setUpdatedBy(jwt.getSubject());
        e = assetRepository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponse.from(e));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Deactivate an asset (soft delete)")
    public AssetResponse deactivate(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        AssetEntity e = assetRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + id));
        e.setLifecycleStatus("INACTIVE");
        e.setUpdatedBy(jwt.getSubject());
        return AssetResponse.from(assetRepository.save(e));
    }
}
