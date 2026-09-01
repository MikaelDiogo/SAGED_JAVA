package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramInitDataValidator;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "saged-telegram-miniapp")
public class TelegramMiniAppController {

    private final TelegramRequesterRepository requesterRepository;
    private final DemandService demandService;
    private final TelegramInitDataValidator initDataValidator;

    public TelegramMiniAppController(TelegramRequesterRepository requesterRepository,
                                      DemandService demandService,
                                      TelegramInitDataValidator initDataValidator) {
        this.requesterRepository = requesterRepository;
        this.demandService = demandService;
        this.initDataValidator = initDataValidator;
    }

    @GetMapping(value = "/telegram/app", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> miniApp(HttpServletResponse response) throws IOException {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        response.setHeader("Cache-Control", "no-store");
        byte[] html = StreamUtils.copyToByteArray(new ClassPathResource("saged-miniapp/app.html").getInputStream());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping(value = "/telegram/app/info", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> infoPage(HttpServletResponse response) throws IOException {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        response.setHeader("Cache-Control", "no-store");
        byte[] html = StreamUtils.copyToByteArray(new ClassPathResource("saged-miniapp/info.html").getInputStream());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    // /api/v1/saged/webhooks/** is public via DevHostSecurityConfig
    @PostMapping("/api/v1/saged/webhooks/miniapp/demand")
    public ResponseEntity<Map<String, String>> createDemand(@RequestBody MiniAppDemandRequest request,
                                                             HttpServletResponse response) {
        response.setHeader("ngrok-skip-browser-warning", "69420");

        String telegramUserId = initDataValidator.extractUserId(request.getInitData());

        var opt = requesterRepository.findByTelegramChatId(telegramUserId);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not registered in SAGED");
        }
        if (opt.get().getStatus() != TelegramRequesterStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is not active");
        }
        TelegramRequesterEntity requester = opt.get();

        String description = (request.getDescription() != null && !request.getDescription().isBlank())
            ? request.getDescription()
            : request.getTitle();

        DemandEntity demand = demandService.create(
            request.getTitle(),
            description,
            request.getSpecialtyCode(),
            null,
            requester.getId(),
            requester.getDepartmentId(),
            "telegram-app:" + telegramUserId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("protocol", demand.getProtocol()));
    }

    public static class MiniAppDemandRequest {
        private String initData;
        private String specialtyCode;
        private String title;
        private String description;

        public String getInitData()              { return initData; }
        public void setInitData(String v)        { this.initData = v; }
        public String getSpecialtyCode()         { return specialtyCode; }
        public void setSpecialtyCode(String v)   { this.specialtyCode = v; }
        public String getTitle()                 { return title; }
        public void setTitle(String v)           { this.title = v; }
        public String getDescription()           { return description; }
        public void setDescription(String v)     { this.description = v; }
    }
}
