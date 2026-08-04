package br.gov.crateus.bcm.saged.api.dto;

import br.gov.crateus.bcm.saged.infrastructure.entity.SystemNotificationEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SystemNotificationResponse {

    private UUID id;
    private String message;
    private String type;
    private boolean read;
    private UUID demandId;
    private String demandProtocol;
    private OffsetDateTime createdAt;

    public static SystemNotificationResponse from(SystemNotificationEntity e) {
        var r = new SystemNotificationResponse();
        r.id = e.getId();
        r.message = e.getMessage();
        r.type = e.getType();
        r.read = e.isRead();
        r.demandId = e.getDemandId();
        r.demandProtocol = e.getDemandProtocol();
        r.createdAt = e.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return read; }
    public UUID getDemandId() { return demandId; }
    public String getDemandProtocol() { return demandProtocol; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
