package br.gov.crateus.bcm.saged.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "saged", name = "telegram_technicians")
public class TelegramTechnicianEntity extends SagedAuditableEntity {

    @Column(name = "telegram_user_id", length = 128, nullable = false, unique = true)
    private String telegramUserId;

    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private UUID keycloakUserId;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TelegramTechnicianStatus status = TelegramTechnicianStatus.ACTIVE;

    public String getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(String v) { this.telegramUserId = v; }

    public UUID getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(UUID v) { this.keycloakUserId = v; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }

    public TelegramTechnicianStatus getStatus() { return status; }
    public void setStatus(TelegramTechnicianStatus v) { this.status = v; }
}
