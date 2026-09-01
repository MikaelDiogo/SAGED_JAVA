package br.gov.crateus.bcm.saged.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(schema = "saged", name = "telegram_sessions")
public class TelegramSessionEntity {

    @Id
    @Column(name = "telegram_user_id", length = 128, nullable = false)
    private String telegramUserId;

    @Column(name = "state", length = 32, nullable = false)
    private String state;

    @Column(name = "specialty_code", length = 32)
    private String specialtyCode;

    @Column(name = "specialty_name", length = 128)
    private String specialtyName;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    public String getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(String v) { this.telegramUserId = v; }

    public String getState() { return state; }
    public void setState(String v) { this.state = v; }

    public String getSpecialtyCode() { return specialtyCode; }
    public void setSpecialtyCode(String v) { this.specialtyCode = v; }

    public String getSpecialtyName() { return specialtyName; }
    public void setSpecialtyName(String v) { this.specialtyName = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v) { this.expiresAt = v; }
}
