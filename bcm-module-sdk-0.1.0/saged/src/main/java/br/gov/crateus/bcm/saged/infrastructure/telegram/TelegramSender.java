package br.gov.crateus.bcm.saged.infrastructure.telegram;

import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TelegramSender {

    private static final String BASE_URL = "https://api.telegram.org";

    private final RestClient restClient;
    private final String botToken;

    public TelegramSender(SagedTelegramProperties props) {
        this.botToken = props.getBotToken();
        this.restClient = RestClient.create(BASE_URL);
    }

    public void sendMessage(long chatId, String text) {
        restClient.post()
            .uri("/bot{token}/sendMessage", botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "Markdown"))
            .retrieve()
            .toBodilessEntity();
    }
}
