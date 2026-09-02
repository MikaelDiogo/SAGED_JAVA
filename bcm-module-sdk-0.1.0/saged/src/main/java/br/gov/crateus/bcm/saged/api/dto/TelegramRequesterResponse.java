package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TelegramRequesterResponse {

    private UUID id;
    private String telegramChatId;
    private String phoneNumber;
    private String displayName;
    private UUID departmentId;
    private String status;
    private boolean active;
    private OffsetDateTime createdAt;
    private String createdBy;

    public static TelegramRequesterResponse from(TelegramRequesterEntity e) {
        var r = new TelegramRequesterResponse();
        r.id = e.getId();
        r.telegramChatId = e.getTelegramChatId();
        r.phoneNumber = e.getPhoneNumber();
        r.displayName = e.getDisplayName();
        r.departmentId = e.getDepartmentId();
        r.status = e.getStatus().name();
        r.active = e.getStatus() == TelegramRequesterStatus.ACTIVE;
        r.createdAt = e.getCreatedAt();
        r.createdBy = e.getCreatedBy();
        return r;
    }

    public UUID getId() { return id; }
    public String getTelegramChatId() { return telegramChatId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDisplayName() { return displayName; }
    public UUID getDepartmentId() { return departmentId; }
    public String getStatus() { return status; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
