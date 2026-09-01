package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.CreateUserSpecialtyRequest;
import br.gov.crateus.bcm.saged.api.dto.UserSpecialtyResponse;
import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.UserSpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.UserSpecialtyRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/saged/user-specialties")
@Tag(name = "saged-user-specialties")
public class UserSpecialtyController {

    private final UserSpecialtyRepository userSpecialtyRepository;
    private final SpecialtyRepository specialtyRepository;

    public UserSpecialtyController(UserSpecialtyRepository userSpecialtyRepository,
                                    SpecialtyRepository specialtyRepository) {
        this.userSpecialtyRepository = userSpecialtyRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List user-specialty assignments — filter by userId or specialtyId")
    public Page<UserSpecialtyResponse> list(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID specialtyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (userId != null) {
            return userSpecialtyRepository.findByUserIdAndLifecycleStatus(userId, "ACTIVE", pageable).map(UserSpecialtyResponse::from);
        }
        if (specialtyId != null) {
            return userSpecialtyRepository.findBySpecialtyIdAndLifecycleStatus(specialtyId, "ACTIVE", pageable).map(UserSpecialtyResponse::from);
        }
        return userSpecialtyRepository.findAllByLifecycleStatus("ACTIVE", pageable).map(UserSpecialtyResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Assign a user to a specialty (ADMIN_GERAL only)")
    @Transactional
    public ResponseEntity<UserSpecialtyResponse> create(
            @RequestBody @Valid CreateUserSpecialtyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        SpecialtyEntity specialty = specialtyRepository.findById(request.getSpecialtyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specialty not found: " + request.getSpecialtyId()));

        if (userSpecialtyRepository.existsByUserIdAndSpecialtyIdAndLifecycleStatus(request.getUserId(), request.getSpecialtyId(), "ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already assigned to this specialty");
        }

        UserSpecialtyEntity e = new UserSpecialtyEntity();
        e.setUserId(request.getUserId());
        e.setSpecialty(specialty);
        e.setCreatedBy(jwt.getSubject());
        e.setUpdatedBy(jwt.getSubject());
        e = userSpecialtyRepository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserSpecialtyResponse.from(e));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Deactivate a user-specialty assignment — soft delete (ADMIN_GERAL only)")
    @Transactional
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UserSpecialtyEntity e = userSpecialtyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User-specialty assignment not found: " + id));
        e.setLifecycleStatus("INACTIVE");
        e.setUpdatedBy(jwt.getSubject());
        userSpecialtyRepository.save(e);
        return ResponseEntity.noContent().build();
    }
}
