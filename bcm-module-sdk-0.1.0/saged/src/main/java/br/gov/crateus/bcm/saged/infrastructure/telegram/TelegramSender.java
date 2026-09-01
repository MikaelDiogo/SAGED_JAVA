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
    private final String miniAppBaseUrl;

    public TelegramSender(SagedTelegramProperties props) {
        this.botToken = props.getBotToken();
        this.miniAppBaseUrl = props.getMiniAppBaseUrl();
        this.restClient = RestClient.create(BASE_URL);
    }

    public void sendMessage(long chatId, String text) {
        post(Map.of("chat_id", chatId, "text", text, "parse_mode", "MarkdownV2"));
    }

    public void sendMainMenu(long chatId) {
        Map<String, Object> sobreBtn = hasMiniApp()
            ? Map.of("text", "Sobre o SAGED", "web_app", Map.of("url", miniAppBaseUrl + "/telegram/info"))
            : Map.of("text", "Sobre o SAGED", "callback_data", "sobre_saged");
        post(Map.of(
            "chat_id", chatId,
            "text", "*Bem\\-vindo ao SAGED*\n\nSistema de Suporte de TI da Prefeitura de Crateús\\.\nSelecione uma opcao abaixo:",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(sobreBtn),
                    List.of(Map.of("text", "Validar meu numero", "callback_data", "validar_numero"))
                )
            )
        ));
    }

    public void sendContactRequest(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Toque no botao abaixo para compartilhar seu numero\\. Ele sera enviado ao sistema para verificacao\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "keyboard", List.of(
                    List.of(Map.of("text", "Compartilhar meu numero", "request_contact", true))
                ),
                "resize_keyboard", true,
                "one_time_keyboard", true
            )
        ));
    }

    public void sendNotRegisteredMessage(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Seu numero nao esta cadastrado no SAGED\\.\n\n" +
                    "Solicite ao *administrador do seu setor* que cadastre o seu numero para liberar o acesso\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of("remove_keyboard", true)
        ));
    }

    public void sendPendingMessage(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Seu numero esta cadastrado, porem o acesso ainda nao foi liberado\\. Aguarde o administrador ativar seu cadastro\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of("remove_keyboard", true)
        ));
    }

    public void sendApprovedMenu(long chatId) {
        Map<String, Object> chamadoBtn = hasMiniApp()
            ? Map.of("text", "Abrir Chamado", "web_app", Map.of("url", miniAppBaseUrl + "/telegram/app"))
            : Map.of("text", "Abrir Chamado", "callback_data", "abrir_chamado");
        post(Map.of(
            "chat_id", chatId,
            "text", "Acesso autorizado\\! Selecione uma opcao:",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(chamadoBtn),
                    List.of(Map.of("text", "Minhas Demandas", "callback_data", "minhas_demandas")),
                    List.of(Map.of("text", "Consultar Status", "callback_data", "consultar_status"))
                )
            )
        ));
    }

    public void sendSpecialtyMenu(long chatId, List<Map<String, String>> specialties) {
        List<List<Map<String, Object>>> keyboard = specialties.stream()
            .map(s -> List.of(Map.<String, Object>of(
                "text", s.get("name"),
                "callback_data", "specialty_" + s.get("code")
            )))
            .toList();
        post(Map.of(
            "chat_id", chatId,
            "text", "Selecione o tipo de chamado:",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of("inline_keyboard", keyboard)
        ));
    }

    public void sendAskTitle(long chatId, String specialtyName) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Tipo selecionado: *" + escape(specialtyName) + "*\n\nDigite o *titulo* do chamado \\(minimo 12 caracteres\\):",
            "parse_mode", "MarkdownV2"
        ));
    }

    public void sendAskDescription(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Digite a *descricao* do chamado \\(minimo 20 caracteres\\):\n_Ou envie_ `/pular` _para usar o titulo como descricao\\._",
            "parse_mode", "MarkdownV2"
        ));
    }

    public void sendSobreMessage(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "*SAGED \\- Sistema de Atendimento e Gestao de Demandas*\n\n" +
                    "O SAGED e o sistema de suporte de TI da Prefeitura de Crateús\\. " +
                    "Ele permite abrir chamados de suporte tecnico, acompanhar o status e receber atualizacoes diretamente pelo Telegram\\.\n\n" +
                    "Para comecar, valide o seu numero de telefone cadastrado\\.",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(Map.of("text", "Validar meu numero", "callback_data", "validar_numero"))
                )
            )
        ));
    }

    public void sendLinkCodeMessage(long chatId, String code) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Seu codigo de vinculo de tecnico e:\n\n*`" + code + "`*\n\n" +
                    "Acesse o painel SAGED e informe esse codigo na aba *Meu Telegram*\\. " +
                    "Valido por *10 minutos*\\.",
            "parse_mode", "MarkdownV2"
        ));
    }

    public void sendTechnicianMenu(long chatId) {
        if (!hasMiniApp()) return;
        post(Map.of(
            "chat_id", chatId,
            "text", "Area do Tecnico disponivel\\:",
            "parse_mode", "MarkdownV2",
            "reply_markup", Map.of(
                "inline_keyboard", List.of(
                    List.of(Map.of("text", "Area do Tecnico", "web_app",
                        Map.of("url", miniAppBaseUrl + "/telegram/tech")))
                )
            )
        ));
    }

    public void sendAskProtocol(long chatId) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Digite o *protocolo* do chamado que deseja consultar:\n_Exemplo: 2026\\-MANUT\\-00001_",
            "parse_mode", "MarkdownV2"
        ));
    }

    public void sendDemandAssignedNotification(long chatId, String protocol, String technicianName) {
        post(Map.of(
            "chat_id", chatId,
            "text", "Seu chamado *" + escape(protocol) + "* foi assumido pelo tecnico *" + escape(technicianName) +
                    "* e esta atualmente *Em Andamento*\\.\n\nPosteriormente voce recebera mais informacoes\\.",
            "parse_mode", "MarkdownV2"
        ));
    }

    public void sendDemandConcludedNotification(long chatId, String protocol, String technicianName, String justification) {
        String msg = "Seu chamado *" + escape(protocol) + "* foi *Concluido* pelo tecnico *" + escape(technicianName) + "*\\.";
        if (justification != null && !justification.isBlank()) {
            msg += "\n\n*Relatorio tecnico:*\n" + escape(justification);
        }
        post(Map.of("chat_id", chatId, "text", msg, "parse_mode", "MarkdownV2"));
    }

    public void sendDemandInterruptedNotification(long chatId, String protocol, String technicianName, String justification) {
        String msg = "Seu chamado *" + escape(protocol) + "* foi *Interrompido* pelo tecnico *" + escape(technicianName) + "*\\.";
        if (justification != null && !justification.isBlank()) {
            msg += "\n\n*Justificativa:*\n" + escape(justification);
        }
        msg += "\n\nPara mais informacoes entre em contato com a TI\\.";
        post(Map.of("chat_id", chatId, "text", msg, "parse_mode", "MarkdownV2"));
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

    private boolean hasMiniApp() {
        return miniAppBaseUrl != null && !miniAppBaseUrl.isBlank();
    }

    public static String escape(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }
}
