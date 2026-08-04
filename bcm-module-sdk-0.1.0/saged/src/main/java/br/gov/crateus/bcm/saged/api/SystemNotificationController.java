package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.api.dto.SystemNotificationResponse;
import br.gov.crateus.bcm.saged.infrastructure.entity.SystemNotificationEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.SystemNotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saged/notifications")
@Tag(name = "saged-notifications")
public class SystemNotificationController {

    private final SystemNotificationRepository repository;

    public SystemNotificationController(SystemNotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "List notifications for the current user")
    public List<SystemNotificationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return repository.findForUser(userId).stream()
            .map(SystemNotificationResponse::from)
            .toList();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Count unread notifications for the current user")
    public long countUnread(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return repository.countUnreadForUser(userId);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        repository.findById(id).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    @Operation(summary = "Mark all notifications as read for the current user")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<SystemNotificationEntity> unread = repository.findForUser(userId).stream()
            .filter(n -> !n.isRead())
            .toList();
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
        return ResponseEntity.noContent().build();
    }
}
