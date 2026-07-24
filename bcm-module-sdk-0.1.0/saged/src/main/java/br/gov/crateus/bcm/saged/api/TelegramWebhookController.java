package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.application.TelegramBotService;
import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import br.gov.crateus.bcm.saged.infrastructure.telegram.dto.TelegramUpdate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saged/telegram")
@Tag(name = "saged-telegram")
public class TelegramWebhookController {

    private final TelegramBotService botService;
    private final SagedTelegramProperties props;

    public TelegramWebhookController(TelegramBotService botService, SagedTelegramProperties props) {
        this.botService = botService;
        this.props = props;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Telegram webhook — recebe updates do bot")
    public ResponseEntity<Void> webhook(
            @RequestBody TelegramUpdate update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret) {
        String expected = props.getWebhookSecret();
        if (expected != null && !expected.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        botService.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}
