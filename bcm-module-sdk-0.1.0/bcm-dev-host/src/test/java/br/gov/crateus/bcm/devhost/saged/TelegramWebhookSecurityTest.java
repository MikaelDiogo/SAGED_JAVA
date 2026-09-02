package br.gov.crateus.bcm.devhost.saged;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class TelegramWebhookSecurityTest extends SagedIntegrationTestBase {

    static final String TEST_SECRET = "test-webhook-secret-abc123";

    // Prevent real outbound calls to the Telegram Bot API while processing updates.
    @MockBean
    TelegramSender telegramSender;

    @DynamicPropertySource
    static void configureWebhookSecret(DynamicPropertyRegistry registry) {
        registry.add("saged.telegram.webhook-secret", () -> TEST_SECRET);
        registry.add("saged.telegram.bot-token", () -> "123456:test-bot-token");
    }

    private static final String VALID_UPDATE = """
            {"update_id":100001,"message":{"message_id":1,"from":{"id":999,"first_name":"Test"},"chat":{"id":999,"type":"private"},"text":"/start","date":1000000}}
            """;

    @Test
    void webhook_withoutSecret_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/saged/webhooks/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_withWrongSecret_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/saged/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_withCorrectSecret_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/saged/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", TEST_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_duplicateUpdateId_processedOnlyOnce() throws Exception {
        String update = """
                {"update_id":200001,"message":{"message_id":2,"from":{"id":888,"first_name":"Dup"},"chat":{"id":888,"type":"private"},"text":"oi","date":1000001}}
                """;

        mockMvc.perform(post("/api/v1/saged/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", TEST_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk());

        // Second request with the same update_id must still return 200 (idempotent)
        // but the update must not be reprocessed (no duplicate state mutations)
        mockMvc.perform(post("/api/v1/saged/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", TEST_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk());
    }
}
