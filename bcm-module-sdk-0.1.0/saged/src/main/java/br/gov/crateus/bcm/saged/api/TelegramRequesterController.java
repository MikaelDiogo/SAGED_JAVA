package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.CreateTelegramRequesterRequest;
import br.gov.crateus.bcm.saged.api.dto.TelegramRequesterResponse;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @PreAuthorize("hasAuthority('SAGED_ADMIN_GERAL')")
    @Operation(summary = "List all Telegram requesters")
    public List<TelegramRequesterResponse> list() {
        return repository.findAll().stream().map(TelegramRequesterResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Register a Telegram requester")
    public ResponseEntity<TelegramRequesterResponse> create(
            @RequestBody @Valid CreateTelegramRequesterRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (repository.existsByTelegramChatId(request.getTelegramChatId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Telegram chat ID already registered: " + request.getTelegramChatId());
        }
        TelegramRequesterEntity e = new TelegramRequesterEntity();
        e.setTelegramChatId(request.getTelegramChatId());
        e.setPhoneNumber(request.getPhoneNumber());
        e.setDisplayName(request.getDisplayName());
        e.setDepartmentId(request.getDepartmentId());
        e.setActive(true);
        e.setCreatedBy(jwt.getSubject());
        e.setUpdatedBy(jwt.getSubject());
        e = repository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(TelegramRequesterResponse.from(e));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Deactivate a Telegram requester")
    public TelegramRequesterResponse deactivate(@PathVariable UUID id,
                                                 @AuthenticationPrincipal Jwt jwt) {
        TelegramRequesterEntity e = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + id));
        e.setActive(false);
        e.setUpdatedBy(jwt.getSubject());
        return TelegramRequesterResponse.from(repository.save(e));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Reactivate a Telegram requester")
    public TelegramRequesterResponse activate(@PathVariable UUID id,
                                               @AuthenticationPrincipal Jwt jwt) {
        TelegramRequesterEntity e = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + id));
        e.setActive(true);
        e.setUpdatedBy(jwt.getSubject());
        return TelegramRequesterResponse.from(repository.save(e));
    }
}
