package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.application.TelegramBotService;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramTechnicianRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/saged/telegram/technicians")
@Tag(name = "saged-telegram-technician")
public class TelegramTechnicianController {

    private final TelegramTechnicianRepository technicianRepository;
    private final TelegramBotService botService;

    public TelegramTechnicianController(TelegramTechnicianRepository technicianRepository,
                                         TelegramBotService botService) {
        this.technicianRepository = technicianRepository;
        this.botService = botService;
    }

    @PostMapping("/link")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_ADMIN_SETOR','SAGED_TECNICO_LIDER','SAGED_TECNICO')")
    public ResponseEntity<Void> linkTelegram(@RequestBody LinkRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());

        if (technicianRepository.existsByKeycloakUserId(keycloakUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Keycloak user already linked to a Telegram account");
        }

        String telegramUserId = botService.consumeLinkCode(request.getCode())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired link code"));

        if (technicianRepository.existsByTelegramUserId(telegramUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telegram account already linked to another user");
        }

        TelegramTechnicianEntity technician = new TelegramTechnicianEntity();
        technician.setTelegramUserId(telegramUserId);
        technician.setKeycloakUserId(keycloakUserId);
        technician.setDisplayName(jwt.getClaimAsString("name"));
        technician.setStatus(TelegramTechnicianStatus.ACTIVE);
        technician.setCreatedBy(jwt.getSubject());
        technician.setUpdatedBy(jwt.getSubject());
        technicianRepository.save(technician);

        try {
            botService.notifyTechnicianLinked(telegramUserId);
        } catch (Exception ignored) {}

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SAGED_ADMIN_GERAL','SAGED_TECNICO_LIDER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                            @AuthenticationPrincipal Jwt jwt) {
        TelegramTechnicianEntity technician = technicianRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician link not found"));
        technician.setStatus(TelegramTechnicianStatus.INACTIVE);
        technician.setUpdatedBy(jwt.getSubject());
        technicianRepository.save(technician);
        return ResponseEntity.noContent().build();
    }

    public static class LinkRequest {
        private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
