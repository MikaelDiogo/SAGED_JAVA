package br.gov.crateus.bcm.saged.application;

import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import br.gov.crateus.bcm.saged.infrastructure.entity.BotProcessedMessageEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramContactEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.BotProcessedMessageRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramContactRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramSender;
import br.gov.crateus.bcm.saged.infrastructure.telegram.dto.TelegramUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TelegramBotService {

    private final DemandService demandService;
    private final TelegramRequesterRepository requesterRepository;
    private final TelegramContactRepository contactRepository;
    private final BotProcessedMessageRepository processedRepository;
    private final TelegramSender sender;
    private final String miniAppUrl;

    public TelegramBotService(DemandService demandService,
                               TelegramRequesterRepository requesterRepository,
                               TelegramContactRepository contactRepository,
                               BotProcessedMessageRepository processedRepository,
                               TelegramSender sender,
                               SagedTelegramProperties props) {
        this.demandService = demandService;
        this.requesterRepository = requesterRepository;
        this.contactRepository = contactRepository;
        this.processedRepository = processedRepository;
        this.sender = sender;
        this.miniAppUrl = deriveMiniAppUrl(props.getWebhookUrl());
    }

    public void handleUpdate(TelegramUpdate update) {
        if (update.getCallbackQuery() != null) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.getMessage() != null) {
            handleMessage(update.getMessage());
        }
    }

    // ── Callback (inline keyboard buttons) ────────────────────────────────────

    private void handleCallbackQuery(TelegramUpdate.CallbackQuery callback) {
        sender.answerCallbackQuery(callback.getId());
        long chatId = callback.getMessage().getChat().getId();
        String data = callback.getData();

        switch (data) {
            case "conhecer_saged" -> sender.sendSagedInfo(chatId);
            case "validar_numero" -> sender.sendContactRequest(chatId);
            default -> sender.sendMainMenu(chatId);
        }
    }

    // ── Message ───────────────────────────────────────────────────────────────

    private void handleMessage(TelegramUpdate.Message message) {
        if (message.getFrom() == null) return;

        String externalId = message.getChat().getId() + ":" + message.getMessageId();
        if (processedRepository.existsByProviderAndExternalMessageId("TELEGRAM", externalId)) return;

        BotProcessedMessageEntity processed = new BotProcessedMessageEntity();
        processed.setProvider("TELEGRAM");
        processed.setExternalMessageId(externalId);
        processed.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
        processedRepository.save(processed);

        upsertContact(message.getFrom(), message.getChat());

        long chatId = message.getChat().getId();
        String telegramUserId = String.valueOf(message.getFrom().getId());

        // Contact share from request_contact button
        if (message.getContact() != null) {
            handleContactShared(chatId, telegramUserId, message.getContact(), message.getFrom());
            return;
        }

        // Mini app form submitted via sendData()
        if (message.getWebAppData() != null) {
            handleWebAppData(chatId, telegramUserId, message.getWebAppData().getData());
            return;
        }

        Optional<TelegramRequesterEntity> requesterOpt = requesterRepository.findByTelegramChatId(telegramUserId);

        if (requesterOpt.isEmpty()) {
            sender.sendMainMenu(chatId);
            return;
        }

        TelegramRequesterEntity requester = requesterOpt.get();

        switch (requester.getStatus()) {
            case PENDING -> sender.sendPendingApprovalMessage(chatId);
            case INACTIVE -> sender.sendMessage(chatId, "❌ Seu acesso foi desativado\\. Contate o administrador\\.");
            case ACTIVE -> handleActiveUserMessage(chatId, message.getText(), requester);
        }
    }

    // ── Mini App data (sendData) ──────────────────────────────────────────────

    private void handleWebAppData(long chatId, String telegramUserId, String data) {
        TelegramRequesterEntity requester = requesterRepository
            .findByTelegramChatId(telegramUserId).orElse(null);
        if (requester == null || requester.getStatus() != TelegramRequesterStatus.ACTIVE) {
            sender.sendMessage(chatId, "Você não tem acesso ativo\\. Use /registrar para solicitar\\.");
            return;
        }
        try {
            JsonNode node = new ObjectMapper().readTree(data);
            String specialtyCode = node.get("specialtyCode").asText();
            String title = node.get("title").asText();
            String description = node.has("description") && !node.get("description").asText().isBlank()
                ? node.get("description").asText()
                : "Aberto via Telegram";
            DemandEntity demand = demandService.create(
                title, description,
                specialtyCode, null,
                requester.getId(), requester.getDepartmentId(),
                "telegram-app:" + telegramUserId
            );
            sender.sendMessage(chatId,
                "✅ Chamado aberto com sucesso\\!\nProtocolo: `" + escape(demand.getProtocol()) + "`");
        } catch (Exception e) {
            sender.sendMessage(chatId, "Erro ao abrir chamado: " + escape(e.getMessage()));
        }
    }

    // ── Phone contact shared ──────────────────────────────────────────────────

    private void handleContactShared(long chatId, String telegramUserId,
                                      TelegramUpdate.Contact contact,
                                      TelegramUpdate.From from) {
        Optional<TelegramRequesterEntity> existing = requesterRepository.findByTelegramChatId(telegramUserId);
        if (existing.isPresent()) {
            switch (existing.get().getStatus()) {
                case PENDING -> sender.sendPendingApprovalMessage(chatId);
                case ACTIVE -> sender.sendApprovedMenu(chatId, miniAppUrl);
                case INACTIVE -> sender.sendMessage(chatId, "❌ Acesso revogado\\. Contate o administrador\\.");
            }
            return;
        }

        String phone = contact.getPhoneNumber();
        String displayName = contact.getFirstName() != null
            ? contact.getFirstName()
            : (from.getFirstName() != null ? from.getFirstName() : from.getUsername());

        TelegramRequesterEntity requester = new TelegramRequesterEntity();
        requester.setTelegramChatId(telegramUserId);
        requester.setPhoneNumber(phone);
        requester.setDisplayName(displayName);
        requester.setStatus(TelegramRequesterStatus.PENDING);
        requester.setCreatedBy("telegram:" + telegramUserId);
        requester.setUpdatedBy("telegram:" + telegramUserId);
        requesterRepository.save(requester);

        sender.sendPendingApprovalMessage(chatId);
    }

    // ── Active user commands ──────────────────────────────────────────────────

    private void handleActiveUserMessage(long chatId, String text, TelegramRequesterEntity requester) {
        if (text == null) {
            sender.sendApprovedMenu(chatId, buildMiniAppUrl(requester.getTelegramChatId()));
            return;
        }
        String trimmed = text.trim();

        if (trimmed.startsWith("/nova")) {
            handleNewDemand(chatId, trimmed, requester);
        } else if (trimmed.equalsIgnoreCase("/minhas")) {
            handleListDemands(chatId, requester);
        } else if (trimmed.startsWith("/status ")) {
            handleStatus(chatId, trimmed);
        } else {
            sender.sendApprovedMenu(chatId, buildMiniAppUrl(requester.getTelegramChatId()));
        }
    }

    private void handleNewDemand(long chatId, String text, TelegramRequesterEntity requester) {
        String args = text.length() > "/nova ".length() ? text.substring("/nova ".length()).trim() : "";
        String[] parts = args.split(" ", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            sender.sendMessage(chatId,
                "Formato: /nova ESPECIALIDADE Título\nEx: /nova MANUT Computador não liga");
            return;
        }
        try {
            DemandEntity demand = demandService.create(
                parts[1].trim(),
                "Criado via Telegram",
                parts[0].toUpperCase(),
                null,
                requester.getId(),
                requester.getDepartmentId(),
                "telegram:" + requester.getTelegramChatId());
            sender.sendMessage(chatId,
                "✅ Demanda criada\\!\nProtocolo: `" + escape(demand.getProtocol()) + "`");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(chatId, "Erro: " + escape(e.getMessage()));
        }
    }

    private void handleListDemands(long chatId, TelegramRequesterEntity requester) {
        List<DemandEntity> demands = demandService.listByRequester(requester.getId());
        if (demands.isEmpty()) {
            sender.sendMessage(chatId, "Você não possui demandas registradas\\.");
            return;
        }
        StringBuilder sb = new StringBuilder("*Suas demandas:*\n\n");
        for (DemandEntity d : demands) {
            sb.append("• `").append(escape(d.getProtocol())).append("` — ")
              .append(escape(d.getTitle())).append(" \\[").append(d.getStatus()).append("\\]\n");
        }
        sender.sendMessage(chatId, sb.toString());
    }

    private void handleStatus(long chatId, String text) {
        String protocol = text.substring("/status ".length()).trim();
        demandService.findByProtocol(protocol).ifPresentOrElse(
            d -> sender.sendMessage(chatId,
                "Protocolo: `" + escape(d.getProtocol()) + "`\n" +
                "Título: " + escape(d.getTitle()) + "\n" +
                "Status: *" + d.getStatus() + "*"),
            () -> sender.sendMessage(chatId, "Protocolo não encontrado: " + escape(protocol))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void upsertContact(TelegramUpdate.From from, TelegramUpdate.Chat chat) {
        String userId = String.valueOf(from.getId());
        TelegramContactEntity contact = contactRepository.findByTelegramUserId(userId)
            .orElseGet(TelegramContactEntity::new);
        contact.setTelegramUserId(userId);
        contact.setChatId(String.valueOf(chat.getId()));
        contactRepository.save(contact);
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }

    public String buildMiniAppUrl(String chatId) {
        return miniAppUrl + "?chatId=" + chatId + "&ngrok-skip-browser-warning=69420";
    }

    private static String deriveMiniAppUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) return "https://example.com/telegram/app";
        try {
            URI uri = URI.create(webhookUrl);
            return uri.getScheme() + "://" + uri.getHost() + "/telegram/app";
        } catch (Exception e) {
            return webhookUrl.replaceFirst("/api/.*", "/telegram/app");
        }
    }
}
