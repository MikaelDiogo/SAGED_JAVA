package br.gov.crateus.bcm.saged.infrastructure.telegram;

import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class TelegramWebhookRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookRegistrar.class);

    private final TelegramSender sender;
    private final SagedTelegramProperties props;

    public TelegramWebhookRegistrar(TelegramSender sender, SagedTelegramProperties props) {
        this.sender = sender;
        this.props = props;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String token = props.getBotToken();
        String url = props.getWebhookUrl();
        if (token == null || token.isBlank() || url == null || url.isBlank()) {
            log.info("Telegram webhook registration skipped — bot-token or webhook-url not configured");
            return;
        }
        try {
            sender.registerWebhook(url, props.getWebhookSecret());
            log.info("Telegram webhook registered: {}", url);
        } catch (Exception e) {
            log.warn("Failed to register Telegram webhook: {}", e.getMessage());
        }
    }
}
