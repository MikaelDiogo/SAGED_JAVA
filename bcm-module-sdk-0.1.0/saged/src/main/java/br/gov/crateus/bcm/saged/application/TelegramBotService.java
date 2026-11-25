package br.gov.crateus.bcm.saged.application;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.BotProcessedMessageEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramContactEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramLinkCodeEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.BotProcessedMessageRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramContactRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramLinkCodeRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramTechnicianRepository;
import br.gov.crateus.bcm.saged.infrastructure.telegram.TelegramSender;
import br.gov.crateus.bcm.saged.infrastructure.telegram.dto.TelegramUpdate;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bot do SAGED. Toda a interação é por botões: menus inline, botão de compartilhar contato
 * e botões web_app que abrem os mini-apps. Qualquer texto digitado apenas reabre o menu.
 */
@Service
@Transactional
public class TelegramBotService {

    private static final int LINK_CODE_TTL_MINUTES = 10;
    private static final int MAX_DEMAND_BUTTONS = 10;
    private static final int BUTTON_TITLE_MAX = 40;

    private final SecureRandom secureRandom = new SecureRandom();

    private final DemandService demandService;
    private final TelegramRequesterRepository requesterRepository;
    private final TelegramTechnicianRepository technicianRepository;
    private final TelegramContactRepository contactRepository;
    private final BotProcessedMessageRepository processedRepository;
    private final TelegramLinkCodeRepository linkCodeRepository;
    private final TelegramSender sender;

    public TelegramBotService(DemandService demandService,
                               TelegramRequesterRepository requesterRepository,
                               TelegramTechnicianRepository technicianRepository,
                               TelegramContactRepository contactRepository,
                               BotProcessedMessageRepository processedRepository,
                               TelegramLinkCodeRepository linkCodeRepository,
                               TelegramSender sender) {
        this.demandService = demandService;
        this.requesterRepository = requesterRepository;
        this.technicianRepository = technicianRepository;
        this.contactRepository = contactRepository;
        this.processedRepository = processedRepository;
        this.linkCodeRepository = linkCodeRepository;
        this.sender = sender;
    }

    // ── Link code helpers ─────────────────────────────────────────────────────

    public String generateLinkCode(String telegramUserId) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        TelegramLinkCodeEntity entity = new TelegramLinkCodeEntity();
        entity.setCode(code);
        entity.setTelegramUserId(telegramUserId);
        entity.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(LINK_CODE_TTL_MINUTES));
        linkCodeRepository.save(entity);
        return code;
    }

    public Optional<String> consumeLinkCode(String code) {
        return linkCodeRepository.findById(code)
            .filter(e -> e.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
            .map(e -> {
                linkCodeRepository.delete(e);
                return e.getTelegramUserId();
            });
    }

    public void notifyTechnicianLinked(String telegramUserId) {
        try {
            sender.sendTechnicianLinked(Long.parseLong(telegramUserId));
        } catch (NumberFormatException ignored) {}
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

    // ── Botões (callback) ─────────────────────────────────────────────────────

    private void handleCallbackQuery(TelegramUpdate.CallbackQuery callback) {
        sender.answerCallbackQuery(callback.getId());
        long chatId = callback.getMessage().getChat().getId();
        String telegramUserId = String.valueOf(callback.getFrom().getId());
        String data = callback.getData() == null ? "" : callback.getData();

        switch (data) {
            case TelegramSender.CB_SOBRE -> { sender.sendSobreMessage(chatId); return; }
            case TelegramSender.CB_VALIDAR -> { sender.sendContactRequest(chatId); return; }
            case TelegramSender.CB_SOU_TECNICO -> { handleTechnician(chatId, telegramUserId); return; }
            case TelegramSender.CB_MENU -> { sendMenuFor(chatId, telegramUserId); return; }
            default -> { }
        }

        Optional<TelegramRequesterEntity> requesterOpt = requesterRepository.findByTelegramChatId(telegramUserId);
        if (requesterOpt.isEmpty() || requesterOpt.get().getStatus() != TelegramRequesterStatus.ACTIVE) {
            sendMenuFor(chatId, telegramUserId);
            return;
        }
        TelegramRequesterEntity requester = requesterOpt.get();

        switch (data) {
            // Só chega aqui quando o mini-app não está configurado (senão o botão é web_app)
            case TelegramSender.CB_ABRIR -> sender.sendMiniAppUnavailable(chatId);
            case TelegramSender.CB_MINHAS -> handleListDemands(chatId, requester);
            case TelegramSender.CB_STATUS -> handlePickOpenDemand(chatId, requester);
            default -> {
                if (data.startsWith(TelegramSender.CB_DEMANDA_PREFIX)) {
                    handleDemandDetail(chatId, requester, data.substring(TelegramSender.CB_DEMANDA_PREFIX.length()));
                } else {
                    sender.sendApprovedMenu(chatId);
                }
            }
        }
    }

    // ── Mensagens recebidas ───────────────────────────────────────────────────

    private void handleMessage(TelegramUpdate.Message message) {
        if (message.getFrom() == null) return;
        upsertContact(message.getFrom(), message.getChat());

        long chatId = message.getChat().getId();
        String telegramUserId = String.valueOf(message.getFrom().getId());

        if (message.getContact() != null) {
            handleContactShared(chatId, telegramUserId, message.getContact());
            return;
        }

        // Nenhum texto é interpretado: qualquer mensagem (inclusive o "Iniciar") reabre o menu certo
        sendMenuFor(chatId, telegramUserId);
    }

    private void sendMenuFor(long chatId, String telegramUserId) {
        Optional<TelegramRequesterEntity> requesterOpt = requesterRepository.findByTelegramChatId(telegramUserId);
        if (requesterOpt.isPresent()) {
            switch (requesterOpt.get().getStatus()) {
                case ACTIVE -> { sender.sendApprovedMenu(chatId); return; }
                case PENDING -> { sender.sendPendingMessage(chatId); return; }
                case INACTIVE -> { }
            }
        }
        if (isActiveTechnician(telegramUserId)) {
            sender.sendTechnicianMenu(chatId);
            return;
        }
        sender.sendMainMenu(chatId);
    }

    private void handleContactShared(long chatId, String telegramUserId, TelegramUpdate.Contact contact) {
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

    // ── Técnico ───────────────────────────────────────────────────────────────

    private void handleTechnician(long chatId, String telegramUserId) {
        if (isActiveTechnician(telegramUserId)) {
            sender.sendTechnicianMenu(chatId);
            return;
        }
        sender.sendTechnicianLinkCode(chatId, generateLinkCode(telegramUserId));
    }

    private boolean isActiveTechnician(String telegramUserId) {
        return technicianRepository.findByTelegramUserId(telegramUserId)
            .filter(t -> t.getStatus() == TelegramTechnicianStatus.ACTIVE)
            .isPresent();
    }

    // ── Chamados do solicitante ───────────────────────────────────────────────

    private void handleListDemands(long chatId, TelegramRequesterEntity requester) {
        List<DemandEntity> demands = demandService.listByRequester(requester.getId());
        if (demands.isEmpty()) {
            sender.sendNoDemands(chatId);
            return;
        }
        sender.sendDemandPicker(chatId,
            "*Minhas demandas*\nToque em um chamado para ver os detalhes:",
            toButtons(demands));
    }

    private void handlePickOpenDemand(long chatId, TelegramRequesterEntity requester) {
        List<DemandEntity> open = demandService.listByRequester(requester.getId()).stream()
            .filter(d -> d.getStatus() != DemandStatus.DONE)
            .toList();
        if (open.isEmpty()) {
            sender.sendNoDemands(chatId);
            return;
        }
        sender.sendDemandPicker(chatId,
            "*Consultar status*\nEscolha o chamado:",
            toButtons(open));
    }

    private void handleDemandDetail(long chatId, TelegramRequesterEntity requester, String rawId) {
        UUID id;
        try {
            id = UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            sender.sendApprovedMenu(chatId);
            return;
        }
        // Só mostra chamados do próprio solicitante
        Optional<DemandEntity> demand = demandService.listByRequester(requester.getId()).stream()
            .filter(d -> d.getId().equals(id))
            .findFirst();
        if (demand.isEmpty()) {
            sender.sendNoDemands(chatId);
            return;
        }
        DemandEntity d = demand.get();
        sender.sendDemandDetail(chatId,
            "*Chamado* `" + TelegramSender.escape(d.getProtocol()) + "`\n\n" +
            "*Título:* " + TelegramSender.escape(d.getTitle()) + "\n" +
            "*Status:* " + translateStatus(d.getStatus()) + "\n" +
            "*Técnico:* " + (d.getAssigneeUserId() != null ? "atribuído" : "_aguardando técnico_"));
    }

    private List<Map<String, String>> toButtons(List<DemandEntity> demands) {
        return demands.stream()
            .limit(MAX_DEMAND_BUTTONS)
            .map(d -> Map.of(
                "id", d.getId().toString(),
                "label", statusIcon(d.getStatus()) + " " + shorten(d.getTitle()) + " · " + translateStatus(d.getStatus())))
            .toList();
    }

    // ── Notificações ──────────────────────────────────────────────────────────

    public void notifyDemandCreated(TelegramRequesterEntity requester, DemandEntity demand) {
        if (requester.getTelegramChatId() == null) return;
        sender.sendDemandCreated(Long.parseLong(requester.getTelegramChatId()), demand.getProtocol(), demand.getTitle());
    }

    public void notifyDemandAssigned(DemandEntity demand, String technicianName) {
        findRequesterChatId(demand).ifPresent(chatId ->
            sender.sendDemandAssignedNotification(chatId, demand.getProtocol(), technicianName));
    }

    public void notifyDemandConcluded(DemandEntity demand, String technicianName, String justification) {
        findRequesterChatId(demand).ifPresent(chatId ->
            sender.sendDemandConcludedNotification(chatId, demand.getProtocol(),
                technicianName != null ? technicianName : "Técnico", justification));
    }

    public void notifyDemandInterrupted(DemandEntity demand, String technicianName, String justification) {
        findRequesterChatId(demand).ifPresent(chatId ->
            sender.sendDemandInterruptedNotification(chatId, demand.getProtocol(),
                technicianName != null ? technicianName : "Técnico", justification));
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

    private static String shorten(String title) {
        if (title == null) return "";
        return title.length() <= BUTTON_TITLE_MAX ? title : title.substring(0, BUTTON_TITLE_MAX - 1) + "…";
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("55") && digits.length() > 11) return "+" + digits;
        return phone;
    }

    private static String statusIcon(DemandStatus status) {
        return switch (status) {
            case TODO -> "🟠";
            case IN_PROGRESS -> "🔵";
            case DONE -> "🟢";
            case INTERRUPTED -> "🔴";
        };
    }

    private static String translateStatus(DemandStatus status) {
        return switch (status) {
            case TODO -> "A Fazer";
            case IN_PROGRESS -> "Em Andamento";
            case DONE -> "Concluído";
            case INTERRUPTED -> "Interrompido";
        };
    }
}
