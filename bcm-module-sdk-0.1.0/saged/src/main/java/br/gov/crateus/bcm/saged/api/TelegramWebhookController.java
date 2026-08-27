package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.application.TelegramBotService;
import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramSender;
import br.gov.crateus.bcm.saged.infrastructure.telegram.dto.TelegramUpdate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saged")
@Tag(name = "saged-telegram")
public class TelegramWebhookController {

    private final TelegramBotService botService;
    private final TelegramSender sender;
    private final SagedTelegramProperties props;

    public TelegramWebhookController(TelegramBotService botService,
                                      TelegramSender sender,
                                      SagedTelegramProperties props) {
        this.botService = botService;
        this.sender = sender;
        this.props = props;
    }

    @PostMapping("/webhooks/telegram")
    @Operation(summary = "Telegram webhook — recebe updates do bot")
    public ResponseEntity<Void> webhook(
            @RequestBody TelegramUpdate update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret) {
        String expected = props.getWebhookSecret();
        if (expected == null || expected.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (secret == null || !MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        botService.handleUpdate(update);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhooks/telegram/register")
    @PreAuthorize("hasRole('SAGED_ADMIN_GERAL')")
    @Operation(summary = "Manually register Telegram webhook URL (ADMIN_GERAL only)")
    public ResponseEntity<Map<String, String>> registerWebhook() {
        String url = props.getWebhookUrl();
        if (props.getBotToken() == null || url == null || url.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "saged.telegram.bot-token or webhook-url not configured"));
        }
        if (props.getWebhookSecret() == null || props.getWebhookSecret().isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "saged.telegram.webhook-secret is required"));
        }
        sender.registerWebhook(url, props.getWebhookSecret());
        return ResponseEntity.ok(Map.of("message", "Webhook registered", "url", url));
    }
}
