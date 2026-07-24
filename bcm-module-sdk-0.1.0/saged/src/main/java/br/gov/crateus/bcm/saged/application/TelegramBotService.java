package br.gov.crateus.bcm.saged.application;

import br.gov.crateus.bcm.saged.infrastructure.entity.BotProcessedMessageEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramContactEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.repository.BotProcessedMessageRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramContactRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramSender;
import br.gov.crateus.bcm.saged.infrastructure.telegram.dto.TelegramUpdate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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

    public TelegramBotService(DemandService demandService,
                               TelegramRequesterRepository requesterRepository,
                               TelegramContactRepository contactRepository,
                               BotProcessedMessageRepository processedRepository,
                               TelegramSender sender) {
        this.demandService = demandService;
        this.requesterRepository = requesterRepository;
        this.contactRepository = contactRepository;
        this.processedRepository = processedRepository;
        this.sender = sender;
    }

    public void handleUpdate(TelegramUpdate update) {
        TelegramUpdate.Message message = update.getMessage();
        if (message == null || message.getText() == null || message.getFrom() == null) return;

        String externalId = message.getChat().getId() + ":" + message.getMessageId();
        if (processedRepository.existsByProviderAndExternalMessageId("TELEGRAM", externalId)) return;

        BotProcessedMessageEntity processed = new BotProcessedMessageEntity();
        processed.setProvider("TELEGRAM");
        processed.setExternalMessageId(externalId);
        processed.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
        processedRepository.save(processed);

        upsertContact(message.getFrom(), message.getChat());

        long chatId = message.getChat().getId();
        String text = message.getText().trim();
        String telegramUserId = String.valueOf(message.getFrom().getId());

        if (text.startsWith("/start") || text.startsWith("/ajuda")) {
            sender.sendMessage(chatId, buildHelp());
            return;
        }

        TelegramRequesterEntity requester = requesterRepository
            .findByTelegramChatIdAndActiveTrue(telegramUserId)
            .orElse(null);

        if (requester == null) {
            sender.sendMessage(chatId,
                "Você não está autorizado a usar este bot. Contate o administrador.");
            return;
        }

        if (text.startsWith("/nova")) {
            handleNewDemand(chatId, text, requester);
        } else if (text.equalsIgnoreCase("/minhas")) {
            handleListDemands(chatId, requester);
        } else if (text.startsWith("/status ")) {
            handleStatus(chatId, text);
        } else {
            sender.sendMessage(chatId, "Comando não reconhecido. Digite /ajuda.");
        }
    }

    private void handleNewDemand(long chatId, String text, TelegramRequesterEntity requester) {
        String args = text.length() > "/nova ".length() ? text.substring("/nova ".length()).trim() : "";
        String[] parts = args.split(" ", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            sender.sendMessage(chatId,
                "Formato: /nova ESPECIALIDADE Título da demanda\n" +
                "Exemplo: /nova HARDWARE Computador não liga");
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
                "Demanda criada com sucesso\\!\nProtocolo: `" + demand.getProtocol() + "`");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(chatId, "Erro: " + e.getMessage());
        }
    }

    private void handleListDemands(long chatId, TelegramRequesterEntity requester) {
        List<DemandEntity> demands = demandService.listByRequester(requester.getId());
        if (demands.isEmpty()) {
            sender.sendMessage(chatId, "Você não possui demandas registradas.");
            return;
        }
        StringBuilder sb = new StringBuilder("*Suas demandas:*\n\n");
        for (DemandEntity d : demands) {
            sb.append("• `").append(d.getProtocol()).append("` — ")
              .append(d.getTitle()).append(" \\[").append(d.getStatus()).append("\\]\n");
        }
        sender.sendMessage(chatId, sb.toString());
    }

    private void handleStatus(long chatId, String text) {
        String protocol = text.substring("/status ".length()).trim();
        demandService.findByProtocol(protocol).ifPresentOrElse(
            d -> sender.sendMessage(chatId,
                "Protocolo: `" + d.getProtocol() + "`\n" +
                "Título: " + d.getTitle() + "\n" +
                "Status: *" + d.getStatus() + "*"),
            () -> sender.sendMessage(chatId, "Protocolo não encontrado: " + protocol)
        );
    }

    private void upsertContact(TelegramUpdate.From from, TelegramUpdate.Chat chat) {
        String userId = String.valueOf(from.getId());
        TelegramContactEntity contact = contactRepository.findByTelegramUserId(userId)
            .orElseGet(TelegramContactEntity::new);
        contact.setTelegramUserId(userId);
        contact.setChatId(String.valueOf(chat.getId()));
        contactRepository.save(contact);
    }

    private String buildHelp() {
        return "*SAGED Bot*\n\n" +
            "Comandos disponíveis:\n\n" +
            "/nova ESPECIALIDADE Título — Criar nova demanda\n" +
            "/minhas — Listar suas demandas\n" +
            "/status PROTOCOLO — Consultar status de uma demanda\n" +
            "/ajuda — Exibir esta mensagem";
    }
}
