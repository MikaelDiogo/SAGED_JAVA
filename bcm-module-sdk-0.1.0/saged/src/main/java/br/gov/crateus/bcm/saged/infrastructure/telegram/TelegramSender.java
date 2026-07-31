package br.gov.crateus.bcm.saged.infrastructure.telegram;

import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import java.util.List;
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
        post(Map.of("chat_id", chatId, "text", text, "parse_mode", "MarkdownV2"));
    }

    public void sendMainMenu(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "👋 *Bem\\-vindo ao SAGED*\n\nSistema de Gerenciamento de Demandas\\. Siga os passos abaixo:",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(
                        Map.of("text", "1️⃣  Conhecer o SAGED", "callback_data", "conhecer_saged"),
                        Map.of("text", "2️⃣  Validar Número", "callback_data", "validar_numero")
                    )
                )
            )
        ));
    }

    public void sendSagedInfo(long chatId) {
        String text =
            "📋 *O que é o SAGED?*\n\n" +
            "O SAGED é o sistema de gerenciamento de demandas de TI da Prefeitura de Crateús\\.\n\n" +
            "Você pode abrir chamados para:\n" +
            "• 🔧 *Manutenção* — hardware, equipamentos e periféricos\n" +
            "• 🌐 *Internet* — conectividade e redes\n\n" +
            "Cada chamado recebe um protocolo único e pode ser acompanhado em tempo real\\.\n\n" +
            "Agora, *valide seu número* para solicitar acesso:";
        post(Map.of(
            "chat_id", chatId,
            "text", text,
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(Map.of("text", "2️⃣  Validar Número", "callback_data", "validar_numero"))
                )
            )
        ));
    }

    public void sendContactRequest(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "📱 Toque no botão abaixo para compartilhar seu número\\. Ele será enviado automaticamente ao sistema para aprovação\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "keyboard", List.of(
                    List.of(Map.of("text", "📱 Compartilhar meu número", "request_contact", true))
                ),
                "resize_keyboard", true,
                "one_time_keyboard", true
            )
        ));
    }

    public void sendPendingApprovalMessage(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "⏳ Sua solicitação está *aguardando aprovação* de um administrador\\. Você será notificado quando aprovado\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of("remove_keyboard", true)
        ));
    }

    public void sendApprovedMenu(long chatId, String webAppUrl) {
        post(Map.of(
            "chat_id", chatId,
            "text", "✅ Acesso liberado\\! Use o botão no teclado para abrir um chamado:",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "keyboard", List.of(
                    List.of(Map.of("text", "🎫  Abrir Demanda", "web_app", Map.of("url", webAppUrl)))
                ),
                "resize_keyboard", true,
                "persistent", true
            )
        ));
    }

    public void answerCallbackQuery(String callbackQueryId) {
        restClient.post()
            .uri("/bot{token}/answerCallbackQuery", botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("callback_query_id", callbackQueryId))
            .retrieve()
            .toBodilessEntity();
    }

    public void registerWebhook(String webhookUrl, String secret) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("url", webhookUrl);
        if (secret != null && !secret.isBlank()) {
            body.put("secret_token", secret);
        }
        restClient.post()
            .uri("/bot{token}/setWebhook", botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }

    private void post(Object body) {
        restClient.post()
            .uri("/bot{token}/sendMessage", botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }
}
