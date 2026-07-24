package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.devhost.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "saged", name = "telegram_demand_requesters")
public class TelegramRequesterEntity extends SdkAuditableEntity {

    @Column(name = "telegram_chat_id", length = 128, nullable = false)
    private String telegramChatId;

    @Column(name = "phone_number", length = 64, nullable = false)
    private String phoneNumber;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
