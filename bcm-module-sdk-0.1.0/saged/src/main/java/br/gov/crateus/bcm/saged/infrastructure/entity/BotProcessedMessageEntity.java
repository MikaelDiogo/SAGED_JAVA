package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.sdk.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(schema = "saged", name = "bot_processed_messages")
public class BotProcessedMessageEntity extends SdkAuditableEntity {

    @Column(name = "provider", length = 32, nullable = false)
    private String provider = "TELEGRAM";

    @Column(name = "external_message_id", length = 128, nullable = false)
    private String externalMessageId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalMessageId() { return externalMessageId; }
    public void setExternalMessageId(String externalMessageId) { this.externalMessageId = externalMessageId; }

    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
