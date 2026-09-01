package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramTechnicianRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramInitDataValidator;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "saged-telegram-technician-miniapp")
public class TechnicianMiniAppController {

    private final TelegramTechnicianRepository technicianRepository;
    private final DemandService demandService;
    private final TelegramInitDataValidator initDataValidator;

    public TechnicianMiniAppController(TelegramTechnicianRepository technicianRepository,
                                        DemandService demandService,
                                        TelegramInitDataValidator initDataValidator) {
        this.technicianRepository = technicianRepository;
        this.demandService = demandService;
        this.initDataValidator = initDataValidator;
    }

    @GetMapping(value = "/telegram/app/tech", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> techMiniApp(HttpServletResponse response) throws IOException {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        response.setHeader("Cache-Control", "no-store");
        byte[] html = StreamUtils.copyToByteArray(
            new ClassPathResource("saged-miniapp/technician.html").getInputStream());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/api/v1/saged/webhooks/miniapp/tech/demands")
    public ResponseEntity<List<Map<String, Object>>> listDemands(@RequestBody InitDataRequest request,
                                                                  HttpServletResponse response) {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        TelegramTechnicianEntity technician = resolveTechnician(request.getInitData());
        List<DemandEntity> demands = demandService.listByAssignee(technician.getKeycloakUserId());
        List<Map<String, Object>> body = demands.stream().map(this::toSummary).toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/api/v1/saged/webhooks/miniapp/tech/demands/{id}/status")
    public ResponseEntity<Map<String, Object>> changeStatus(@PathVariable UUID id,
                                                             @RequestBody StatusChangeRequest request,
                                                             HttpServletResponse response) {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        TelegramTechnicianEntity technician = resolveTechnician(request.getInitData());

        DemandEntity demand = demandService.findById(id);
        if (!technician.getKeycloakUserId().equals(demand.getAssigneeUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demand not assigned to this technician");
        }

        DemandStatus newStatus;
        try {
            newStatus = DemandStatus.valueOf(request.getNewStatus());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getNewStatus());
        }

        DemandEntity updated = demandService.changeStatus(
            id, newStatus, request.getJustification(),
            "telegram-tech:" + technician.getTelegramUserId()
        );
        return ResponseEntity.ok(toSummary(updated));
    }

    private TelegramTechnicianEntity resolveTechnician(String initData) {
        String telegramUserId = initDataValidator.extractUserId(initData);
        TelegramTechnicianEntity technician = technicianRepository.findByTelegramUserId(telegramUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Telegram account not linked to a technician. Send /vincular in the bot."));
        if (technician.getStatus() != TelegramTechnicianStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Technician account is not active");
        }
        return technician;
    }

    private Map<String, Object> toSummary(DemandEntity d) {
        return Map.of(
            "id", d.getId().toString(),
            "protocol", d.getProtocol(),
            "title", d.getTitle(),
            "description", d.getDescription(),
            "status", d.getStatus().name(),
            "specialtyCode", d.getSpecialty() != null ? d.getSpecialty().getCode() : "",
            "specialtyName", d.getSpecialty() != null ? d.getSpecialty().getName() : "",
            "assetTag", d.getAssetTag() != null ? d.getAssetTag() : ""
        );
    }

    public static class InitDataRequest {
        private String initData;
        public String getInitData() { return initData; }
        public void setInitData(String v) { this.initData = v; }
    }

    public static class StatusChangeRequest {
        private String initData;
        private String newStatus;
        private String justification;
        public String getInitData() { return initData; }
        public void setInitData(String v) { this.initData = v; }
        public String getNewStatus() { return newStatus; }
        public void setNewStatus(String v) { this.newStatus = v; }
        public String getJustification() { return justification; }
        public void setJustification(String v) { this.justification = v; }
    }
}
