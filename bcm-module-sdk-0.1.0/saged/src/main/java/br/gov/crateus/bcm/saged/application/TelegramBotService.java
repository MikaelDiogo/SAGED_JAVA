package br.gov.crateus.bcm.saged.application;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.BotProcessedMessageEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.SpecialtyEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramContactEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.BotProcessedMessageRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.SpecialtyRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramContactRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramSender;
import br.gov.crateus.bcm.saged.infrastructure.telegram.dto.TelegramUpdate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TelegramBotService {

    private record TelegramSession(String state, String specialtyCode, String specialtyName, String title) {}

    private final Map<String, TelegramSession> sessions = new ConcurrentHashMap<>();

    private final DemandService demandService;
    private final TelegramRequesterRepository requesterRepository;
    private final TelegramContactRepository contactRepository;
    private final BotProcessedMessageRepository processedRepository;
    private final SpecialtyRepository specialtyRepository;
    private final TelegramSender sender;

    public TelegramBotService(DemandService demandService,
                               TelegramRequesterRepository requesterRepository,
                               TelegramContactRepository contactRepository,
                               BotProcessedMessageRepository processedRepository,
                               SpecialtyRepository specialtyRepository,
                               TelegramSender sender) {
        this.demandService = demandService;
        this.requesterRepository = requesterRepository;
        this.contactRepository = contactRepository;
        this.processedRepository = processedRepository;
        this.specialtyRepository = specialtyRepository;
        this.sender = sender;
    }

    public void handleUpdate(TelegramUpdate update) {
        if (update.getUpdateId() != null) {
            String key = String.valueOf(update.getUpdateId());
            if (processedRepository.existsByProviderAndExternalMessageId("TELEGRAM", key)) return;
            BotProcessedMessageEntity processed = new BotProcessedMessageEntity();
            processed.setProvider("TELEGRAM");
            processed.setExternalMessageId(key);
            processed.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            processedRepository.save(processed);
        }
        if (update.getCallbackQuery() != null) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.getMessage() != null) {
            handleMessage(update.getMessage());
        }
    }

    // ── Callback (inline keyboard) ─────────────────────────────────────────────

    private void handleCallbackQuery(TelegramUpdate.CallbackQuery callback) {
        sender.answerCallbackQuery(callback.getId());
        long chatId = callback.getMessage().getChat().getId();
        String telegramUserId = String.valueOf(callback.getFrom().getId());
        String data = callback.getData();

        if ("sobre_saged".equals(data)) {
            sender.sendSobreMessage(chatId);
            return;
        }
        if ("validar_numero".equals(data)) {
            sender.sendContactRequest(chatId);
            return;
        }

        Optional<TelegramRequesterEntity> requesterOpt = requesterRepository.findByTelegramChatId(telegramUserId);
        if (requesterOpt.isEmpty() || requesterOpt.get().getStatus() != TelegramRequesterStatus.ACTIVE) {
            sender.sendMainMenu(chatId);
            return;
        }
        TelegramRequesterEntity requester = requesterOpt.get();

        switch (data) {
            case "abrir_chamado" -> handleStartDemand(chatId, telegramUserId);
            case "minhas_demandas" -> handleListDemands(chatId, requester);
            case "consultar_status" -> {
                sessions.put(telegramUserId, new TelegramSession("WAITING_PROTOCOL", null, null, null));
                sender.sendAskProtocol(chatId);
            }
            default -> {
                if (data.startsWith("specialty_")) {
                    String code = data.substring("specialty_".length());
                    handleSpecialtySelected(chatId, telegramUserId, code);
                } else {
                    sender.sendApprovedMenu(chatId);
                }
            }
        }
    }

    // ── Message ───────────────────────────────────────────────────────────────

    private void handleMessage(TelegramUpdate.Message message) {
        if (message.getFrom() == null) return;
        upsertContact(message.getFrom(), message.getChat());

        long chatId = message.getChat().getId();
        String telegramUserId = String.valueOf(message.getFrom().getId());

        if (message.getContact() != null) {
            handleContactShared(chatId, telegramUserId, message.getContact(), message.getFrom());
            return;
        }

        Optional<TelegramRequesterEntity> requesterOpt = requesterRepository.findByTelegramChatId(telegramUserId);

        if (requesterOpt.isEmpty()) {
            sender.sendMainMenu(chatId);
            return;
        }

        TelegramRequesterEntity requester = requesterOpt.get();

        switch (requester.getStatus()) {
            case PENDING -> sender.sendPendingMessage(chatId);
            case INACTIVE -> sender.sendMainMenu(chatId);
            case ACTIVE -> handleActiveUserMessage(chatId, telegramUserId, message.getText(), requester);
        }
    }

    // ── New auth flow: contact shared ─────────────────────────────────────────

    private void handleContactShared(long chatId, String telegramUserId,
                                      TelegramUpdate.Contact contact,
                                      TelegramUpdate.From from) {
        String phone = normalizePhone(contact.getPhoneNumber());

        Optional<TelegramRequesterEntity> byPhone = requesterRepository.findByPhoneNumber(phone);
        if (byPhone.isEmpty()) {
            byPhone = requesterRepository.findByPhoneNumber(contact.getPhoneNumber());
        }

        if (byPhone.isPresent()) {
            TelegramRequesterEntity requester = byPhone.get();
            switch (requester.getStatus()) {
                case ACTIVE -> {
                    if (requester.getTelegramChatId() == null) {
                        requester.setTelegramChatId(telegramUserId);
                        requesterRepository.save(requester);
                    }
                    sender.sendApprovedMenu(chatId);
                }
                case PENDING -> sender.sendPendingMessage(chatId);
                case INACTIVE -> sender.sendNotRegisteredMessage(chatId);
            }
            return;
        }

        sender.sendNotRegisteredMessage(chatId);
    }

    // ── Conversation flow for opening demand ──────────────────────────────────

    private void handleStartDemand(long chatId, String telegramUserId) {
        List<SpecialtyEntity> specialties = specialtyRepository.findAll();
        if (specialties.isEmpty()) {
            sender.sendMessage(chatId, "Nenhuma especialidade disponivel no momento\\.");
            return;
        }
        List<Map<String, String>> list = specialties.stream()
            .map(s -> Map.of("code", s.getCode(), "name", s.getName()))
            .toList();
        sender.sendSpecialtyMenu(chatId, list);
    }

    private void handleSpecialtySelected(long chatId, String telegramUserId, String code) {
        SpecialtyEntity specialty = specialtyRepository.findWithLockByCode(code).orElse(null);
        if (specialty == null) {
            sender.sendMessage(chatId, "Especialidade invalida\\.");
            return;
        }
        sessions.put(telegramUserId, new TelegramSession("WAITING_TITLE", code, specialty.getName(), null));
        sender.sendAskTitle(chatId, specialty.getName());
    }

    // ── Active user text messages ─────────────────────────────────────────────

    private void handleActiveUserMessage(long chatId, String telegramUserId, String text,
                                          TelegramRequesterEntity requester) {
        TelegramSession session = sessions.get(telegramUserId);

        if (session != null) {
            switch (session.state()) {
                case "WAITING_TITLE" -> {
                    if (text == null || text.trim().length() < 12) {
                        sender.sendMessage(chatId, "O titulo deve ter no minimo 12 caracteres\\. Tente novamente:");
                        return;
                    }
                    sessions.put(telegramUserId, new TelegramSession("WAITING_DESCRIPTION",
                        session.specialtyCode(), session.specialtyName(), text.trim()));
                    sender.sendAskDescription(chatId);
                    return;
                }
                case "WAITING_DESCRIPTION" -> {
                    String description;
                    if (text == null || "/pular".equalsIgnoreCase(text.trim())) {
                        description = session.title();
                    } else if (text.trim().length() < 20) {
                        sender.sendMessage(chatId, "A descricao deve ter no minimo 20 caracteres\\. Tente novamente ou envie `/pular`:");
                        return;
                    } else {
                        description = text.trim();
                    }
                    sessions.remove(telegramUserId);
                    createDemandFromSession(chatId, telegramUserId, requester, session, description);
                    return;
                }
                case "WAITING_PROTOCOL" -> {
                    sessions.remove(telegramUserId);
                    handleStatus(chatId, text);
                    return;
                }
            }
        }

        if (text == null) {
            sender.sendApprovedMenu(chatId);
            return;
        }

        String trimmed = text.trim();
        if (trimmed.equalsIgnoreCase("/menu") || trimmed.equalsIgnoreCase("/start")) {
            sender.sendApprovedMenu(chatId);
        } else if (trimmed.equalsIgnoreCase("/minhas")) {
            handleListDemands(chatId, requester);
        } else if (trimmed.startsWith("/status ")) {
            handleStatus(chatId, trimmed.substring("/status ".length()).trim());
        } else {
            sender.sendApprovedMenu(chatId);
        }
    }

    private void createDemandFromSession(long chatId, String telegramUserId,
                                          TelegramRequesterEntity requester,
                                          TelegramSession session, String description) {
        try {
            DemandEntity demand = demandService.create(
                session.title(), description,
                session.specialtyCode(), null,
                requester.getId(), requester.getDepartmentId(),
                "telegram:" + telegramUserId
            );
            sender.sendMessage(chatId,
                "Chamado aberto com sucesso\\!\nProtocolo: `" + TelegramSender.escape(demand.getProtocol()) + "`\n" +
                "Tipo: " + TelegramSender.escape(session.specialtyName()) + "\n" +
                "Status: *A Fazer*\n\nAcompanhe o status pelo menu abaixo\\.");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(chatId, "Erro ao abrir chamado: " + TelegramSender.escape(e.getMessage()));
        }
    }

    private void handleListDemands(long chatId, TelegramRequesterEntity requester) {
        List<DemandEntity> demands = demandService.listByRequester(requester.getId());
        if (demands.isEmpty()) {
            sender.sendMessage(chatId, "Voce nao possui demandas registradas\\.");
            return;
        }
        StringBuilder sb = new StringBuilder("*Suas demandas:*\n\n");
        for (DemandEntity d : demands) {
            sb.append("• `").append(TelegramSender.escape(d.getProtocol())).append("` — ")
              .append(TelegramSender.escape(d.getTitle())).append(" \\[")
              .append(translateStatus(d.getStatus())).append("\\]\n");
        }
        sender.sendMessage(chatId, sb.toString());
    }

    private void handleStatus(long chatId, String protocol) {
        if (protocol == null || protocol.isBlank()) {
            sender.sendMessage(chatId, "Protocolo invalido\\.");
            return;
        }
        demandService.findByProtocol(protocol).ifPresentOrElse(
            d -> sender.sendMessage(chatId,
                "Protocolo: `" + TelegramSender.escape(d.getProtocol()) + "`\n" +
                "Titulo: " + TelegramSender.escape(d.getTitle()) + "\n" +
                "Status: *" + translateStatus(d.getStatus()) + "*\n" +
                (d.getAssigneeUserId() != null ? "Tecnico: atribuido" : "Tecnico: _nao atribuido_")),
            () -> sender.sendMessage(chatId, "Protocolo nao encontrado: `" + TelegramSender.escape(protocol) + "`")
        );
    }

    // ── Notifications from demand controller ──────────────────────────────────

    public void notifyDemandAssigned(DemandEntity demand, String technicianName) {
        findRequesterChatId(demand).ifPresent(chatId ->
            sender.sendDemandAssignedNotification(chatId, demand.getProtocol(), technicianName));
    }

    public void notifyDemandConcluded(DemandEntity demand, String technicianName, String justification) {
        findRequesterChatId(demand).ifPresent(chatId ->
            sender.sendDemandConcludedNotification(chatId, demand.getProtocol(),
                technicianName != null ? technicianName : "Tecnico", justification));
    }

    public void notifyDemandInterrupted(DemandEntity demand, String technicianName, String justification) {
        findRequesterChatId(demand).ifPresent(chatId ->
            sender.sendDemandInterruptedNotification(chatId, demand.getProtocol(),
                technicianName != null ? technicianName : "Tecnico", justification));
    }

    public void notifyApproved(String telegramChatId) {
        long chatId = Long.parseLong(telegramChatId);
        sender.sendApprovedMenu(chatId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Optional<Long> findRequesterChatId(DemandEntity demand) {
        if (demand.getRequesterUserId() == null) return Optional.empty();
        return requesterRepository.findById(demand.getRequesterUserId())
            .filter(r -> r.getStatus() == TelegramRequesterStatus.ACTIVE && r.getTelegramChatId() != null)
            .map(r -> Long.parseLong(r.getTelegramChatId()));
    }

    private void upsertContact(TelegramUpdate.From from, TelegramUpdate.Chat chat) {
        String userId = String.valueOf(from.getId());
        TelegramContactEntity contact = contactRepository.findByTelegramUserId(userId)
            .orElseGet(TelegramContactEntity::new);
        contact.setTelegramUserId(userId);
        contact.setChatId(String.valueOf(chat.getId()));
        contactRepository.save(contact);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("55") && digits.length() > 11) return "+" + digits;
        return phone;
    }

    private static String translateStatus(DemandStatus status) {
        return switch (status) {
            case TODO -> "A Fazer";
            case IN_PROGRESS -> "Em Andamento";
            case DONE -> "Concluido";
            case INTERRUPTED -> "Interrompido";
        };
    }
}
