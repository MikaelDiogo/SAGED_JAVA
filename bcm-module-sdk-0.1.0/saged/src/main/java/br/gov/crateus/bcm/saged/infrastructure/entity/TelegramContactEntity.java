package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.devhost.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "saged", name = "telegram_contacts")
public class TelegramContactEntity extends SdkAuditableEntity {

    @Column(name = "telegram_user_id", length = 128, nullable = false, unique = true)
    private String telegramUserId;

    @Column(name = "chat_id", length = 128, nullable = false)
    private String chatId;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    public String getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(String telegramUserId) { this.telegramUserId = telegramUserId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
