package br.gov.crateus.bcm.saged.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(schema = "saged", name = "telegram_link_codes")
public class TelegramLinkCodeEntity {

    @Id
    @Column(name = "code", length = 6, nullable = false)
    private String code;

    @Column(name = "telegram_user_id", length = 128, nullable = false)
    private String telegramUserId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }

    public String getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(String v) { this.telegramUserId = v; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v) { this.expiresAt = v; }
}
