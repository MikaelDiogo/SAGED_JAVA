package br.gov.crateus.bcm.saged.infrastructure.telegram;

import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Mensagens do bot. Toda a navegação é feita por botões (inline keyboard ou web_app);
 * o bot não depende de comandos nem de texto digitado pelo usuário.
 */
@Component
public class TelegramSender {

    private static final String BASE_URL = "https://api.telegram.org";

    public static final String CB_MENU = "menu";
    public static final String CB_SOBRE = "sobre_saged";
    public static final String CB_VALIDAR = "validar_numero";
    public static final String CB_SOU_TECNICO = "sou_tecnico";
    public static final String CB_ABRIR = "abrir_chamado";
    public static final String CB_MINHAS = "minhas_demandas";
    public static final String CB_STATUS = "consultar_status";
    public static final String CB_DEMANDA_PREFIX = "demanda_";

    private final RestClient restClient;
    private final String botToken;
    private final String miniAppBaseUrl;

    public TelegramSender(SagedTelegramProperties props) {
        this.botToken = props.getBotToken();
        this.miniAppBaseUrl = props.getMiniAppBaseUrl();
        this.restClient = RestClient.create(BASE_URL);
    }

    public void sendMessage(long chatId, String text) {
        post(Map.of("chat_id", chatId, "text", text, "parse_mode", "MarkdownV2"));
    }

    // ── Menus ─────────────────────────────────────────────────────────────────

    public void sendMainMenu(long chatId) {
        Map<String, Object> sobreBtn = hasMiniApp()
            ? webAppButton("Sobre o SAGED", "/telegram/app/info")
            : callbackButton("Sobre o SAGED", CB_SOBRE);
        send(chatId,
            "*Bem\\-vindo ao SAGED*\n\nSistema de Suporte de TI da Prefeitura de Crateús\\.\nToque em uma opção abaixo:",
            List.of(
                List.of(sobreBtn),
                List.of(callbackButton("Validar meu número", CB_VALIDAR)),
                List.of(callbackButton("Sou técnico", CB_SOU_TECNICO))
            ));
    }

    public void sendApprovedMenu(long chatId) {
        Map<String, Object> chamadoBtn = hasMiniApp()
            ? webAppButton("Abrir Chamado", "/telegram/app")
            : callbackButton("Abrir Chamado", CB_ABRIR);
        send(chatId,
            "*Acesso autorizado\\!*\nToque em uma opção:",
            List.of(
                List.of(chamadoBtn),
                List.of(callbackButton("Minhas Demandas", CB_MINHAS)),
                List.of(callbackButton("Consultar Status", CB_STATUS))
            ));
    }

    public void sendTechnicianMenu(long chatId) {
        if (!hasMiniApp()) {
            sendMiniAppUnavailable(chatId);
            return;
        }
        send(chatId,
            "*Área do Técnico*\nToque no botão para ver e atualizar os chamados atribuídos a você\\.",
            List.of(List.of(webAppButton("Área do Técnico", "/telegram/app/tech"))));
    }

    // ── Acesso do solicitante ─────────────────────────────────────────────────

    public void sendContactRequest(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Toque no botão abaixo para compartilhar seu número\\. Ele será enviado ao sistema para verificação\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "keyboard", List.of(
                    List.of(Map.of("text", "Compartilhar meu número", "request_contact", true))
                ),
                "resize_keyboard", true,
                "one_time_keyboard", true
            )
        ));
    }

    public void sendNotRegisteredMessage(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Seu número não está cadastrado no SAGED\\.\n\n" +
                    "Peça ao *administrador do seu setor* para cadastrar o seu número e liberar o acesso\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of("remove_keyboard", true)
        ));
    }

    public void sendPendingMessage(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Seu número está cadastrado, mas o acesso ainda não foi liberado\\. Aguarde o administrador ativar seu cadastro\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of("remove_keyboard", true)
        ));
    }

    public void sendSobreMessage(long chatId) {
        send(chatId,
            "*SAGED \\- Sistema de Atendimento e Gestão de Demandas*\n\n" +
            "O SAGED é o sistema de suporte de TI da Prefeitura de Crateús\\. " +
            "Com ele você abre chamados, acompanha o status e recebe as atualizações aqui no Telegram\\.\n\n" +
            "Para começar, valide o seu número de telefone cadastrado\\.",
            List.of(List.of(callbackButton("Validar meu número", CB_VALIDAR))));
    }

    // ── Chamados do solicitante ───────────────────────────────────────────────

    /** Lista de chamados como botões; cada botão abre o detalhe do chamado. */
    public void sendDemandPicker(long chatId, String text, List<Map<String, String>> demands) {
        List<List<Map<String, Object>>> keyboard = new ArrayList<>();
        for (Map<String, String> d : demands) {
            keyboard.add(List.of(callbackButton(d.get("label"), CB_DEMANDA_PREFIX + d.get("id"))));
        }
        keyboard.add(List.of(callbackButton("« Voltar ao menu", CB_MENU)));
        send(chatId, text, keyboard);
    }

    public void sendDemandDetail(long chatId, String text) {
        send(chatId, text, List.of(
            List.of(callbackButton("« Meus chamados", CB_MINHAS)),
            List.of(callbackButton("Menu", CB_MENU))
        ));
    }

    public void sendNoDemands(long chatId) {
        send(chatId, "Você ainda não tem chamados registrados\\.",
            List.of(List.of(callbackButton("« Voltar ao menu", CB_MENU))));
    }

    public void sendDemandCreated(long chatId, String protocol, String title) {
        send(chatId,
            "*Chamado aberto com sucesso\\!*\n" +
            "Protocolo: `" + escape(protocol) + "`\n" +
            "Título: " + escape(title) + "\n" +
            "Status: *A Fazer*\n\nVocê receberá aqui as atualizações do atendimento\\.",
            List.of(
                List.of(callbackButton("Minhas Demandas", CB_MINHAS)),
                List.of(callbackButton("Menu", CB_MENU))
            ));
    }

    public void sendMiniAppUnavailable(long chatId) {
        send(chatId,
            "Esta opção está indisponível no momento\\. Procure a equipe de TI\\.",
            List.of(List.of(callbackButton("« Voltar ao menu", CB_MENU))));
    }

    // ── Técnico ───────────────────────────────────────────────────────────────

    public void sendTechnicianLinkCode(long chatId, String code) {
        send(chatId,
            "*Vincular conta de técnico*\n\nSeu código é:\n\n*`" + code + "`*\n\n" +
            "No SAGED pelo navegador, clique em *Minha Conta → Vincular Telegram* e informe este código\\. " +
            "Ele vale por *10 minutos*\\.",
            List.of(List.of(callbackButton("Gerar novo código", CB_SOU_TECNICO))));
    }

    public void sendTechnicianLinked(long chatId) {
        if (!hasMiniApp()) {
            sendMessage(chatId, "*Vínculo confirmado\\!*");
            return;
        }
        send(chatId,
            "*Vínculo confirmado\\!*\nAgora você acompanha seus chamados pela Área do Técnico\\.",
            List.of(List.of(webAppButton("Área do Técnico", "/telegram/app/tech"))));
    }

    // ── Notificações ──────────────────────────────────────────────────────────

    public void sendDemandAssignedNotification(long chatId, String protocol, String technicianName) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Seu chamado *" + escape(protocol) + "* foi assumido pelo técnico *" + escape(technicianName) +
                    "* e está *Em Andamento*\\.\n\nVocê receberá novas informações aqui\\.",
            "parse_mode", "MarkdownV2"
        ));
    }

    public void sendDemandConcludedNotification(long chatId, String protocol, String technicianName, String justification) {
        String msg = "Seu chamado *" + escape(protocol) + "* foi *Concluído* pelo técnico *" + escape(technicianName) + "*\\.";
        if (justification != null && !justification.isBlank()) {
            msg += "\n\n*Relatório técnico:*\n" + escape(justification);
        }
        post(Map.of("chat_id", chatId, "text", msg, "parse_mode", "MarkdownV2"));
    }

    public void sendDemandInterruptedNotification(long chatId, String protocol, String technicianName, String justification) {
        String msg = "Seu chamado *" + escape(protocol) + "* foi *Interrompido* pelo técnico *" + escape(technicianName) + "*\\.";
        if (justification != null && !justification.isBlank()) {
            msg += "\n\n*Justificativa:*\n" + escape(justification);
        }
        msg += "\n\nPara mais informações, fale com a equipe de TI\\.";
        post(Map.of("chat_id", chatId, "text", msg, "parse_mode", "MarkdownV2"));
    }

    // ── Infra ─────────────────────────────────────────────────────────────────

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

    private void send(long chatId, String text, List<List<Map<String, Object>>> keyboard) {
        post(Map.of(
            "chat_id", chatId,
            "text", text,
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of("inline_keyboard", keyboard)
        ));
    }

    private static Map<String, Object> callbackButton(String text, String data) {
        return Map.of("text", text, "callback_data", data);
    }

    private Map<String, Object> webAppButton(String text, String path) {
        return Map.of("text", text, "web_app", Map.of("url", miniAppBaseUrl + path));
    }

    private void post(Object body) {
        restClient.post()
            .uri("/bot{token}/sendMessage", botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }

    private boolean hasMiniApp() {
        return miniAppBaseUrl != null && !miniAppBaseUrl.isBlank();
    }

    public static String escape(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }
}
