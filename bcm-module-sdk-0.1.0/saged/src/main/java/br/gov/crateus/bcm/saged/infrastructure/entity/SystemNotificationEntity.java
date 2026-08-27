package br.gov.crateus.bcm.saged.infrastructure.entity;

import br.gov.crateus.bcm.sdk.persistence.SdkAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "saged", name = "system_notifications")
public class SystemNotificationEntity extends SdkAuditableEntity {

    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "type", length = 64, nullable = false)
    private String type;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "demand_id")
    private UUID demandId;

    @Column(name = "demand_protocol", length = 64)
    private String demandProtocol;

    @Column(name = "department_id")
    private UUID departmentId;

    public UUID getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public UUID getDemandId() { return demandId; }
    public void setDemandId(UUID demandId) { this.demandId = demandId; }

    public String getDemandProtocol() { return demandProtocol; }
    public void setDemandProtocol(String demandProtocol) { this.demandProtocol = demandProtocol; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
}
